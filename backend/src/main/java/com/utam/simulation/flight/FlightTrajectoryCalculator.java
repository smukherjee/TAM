package com.utam.simulation.flight;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Calculates realistic flight trajectories for arrivals and departures.
 * Implements FR-023 to FR-025: Realistic altitude/speed profiles and headings.
 */
@Component
public class FlightTrajectoryCalculator {

    private final Random random = new Random();

    // Typical approach/departure parameters
    private static final double APPROACH_START_DISTANCE_KM = 50.0;
    
    @SuppressWarnings("unused") // Reserved for departure trajectory calculations
    private static final double DEPARTURE_END_DISTANCE_KM = 50.0;
    private static final double CRUISE_ALTITUDE_FT = 35000.0;
    private static final double APPROACH_START_ALTITUDE_FT = 10000.0;
    
    @SuppressWarnings("unused") // Reserved for final approach phase calculations
    private static final double FINAL_APPROACH_ALTITUDE_FT = 3000.0;
    private static final double CRUISE_SPEED_KTS = 450.0;
    private static final double APPROACH_SPEED_KTS = 180.0;
    private static final double FINAL_APPROACH_SPEED_KTS = 140.0;
    private static final double DEPARTURE_CLIMB_SPEED_KTS = 250.0;

    /**
     * Calculate the starting position for a new flight.
     */
    public Position calculateStartPosition(double airportLat, double airportLon, 
                                           FlightDataGenerator.FlightType type) {
        // Random approach/departure direction
        double bearing = random.nextDouble() * 360;
        double distance = type == FlightDataGenerator.FlightType.ARRIVING 
                ? APPROACH_START_DISTANCE_KM 
                : 0.5; // Start near airport for departures

        double[] startPos = calculateDestinationPoint(airportLat, airportLon, bearing, distance);

        if (type == FlightDataGenerator.FlightType.ARRIVING) {
            // Arriving - start at approach altitude, heading toward airport
            double headingToAirport = calculateBearing(startPos[0], startPos[1], airportLat, airportLon);
            return new Position(
                    startPos[0],
                    startPos[1],
                    APPROACH_START_ALTITUDE_FT + random.nextDouble() * 2000,
                    APPROACH_SPEED_KTS + random.nextDouble() * 50,
                    headingToAirport
            );
        } else {
            // Departing - start at runway, heading away from airport
            double headingAway = bearing;
            return new Position(
                    airportLat + (random.nextDouble() - 0.5) * 0.01,
                    airportLon + (random.nextDouble() - 0.5) * 0.01,
                    500 + random.nextDouble() * 500, // Just taken off
                    DEPARTURE_CLIMB_SPEED_KTS - 50 + random.nextDouble() * 30,
                    headingAway
            );
        }
    }

    /**
     * Calculate the next position for an active flight (continuous simulation).
     */
    public Position calculateNextPosition(double currentLat, double currentLon,
                                          double currentAlt, double currentSpeed,
                                          double currentHeading,
                                          FlightDataGenerator.FlightType type) {
        // Time step in hours (assuming ~1 second updates)
        double timeStepHours = 1.0 / 3600.0;
        
        // Distance traveled in km
        double distanceNm = currentSpeed * timeStepHours;
        double distanceKm = distanceNm * 1.852;

        // Calculate new position
        double[] newPos = calculateDestinationPoint(currentLat, currentLon, currentHeading, distanceKm);

        double newAlt, newSpeed;
        double newHeading = currentHeading + (random.nextDouble() - 0.5) * 2; // Small heading variation

        if (type == FlightDataGenerator.FlightType.ARRIVING) {
            // Descending profile
            double descentRate = 500 + random.nextDouble() * 200; // ft per update
            newAlt = Math.max(0, currentAlt - descentRate * timeStepHours * 60);
            
            // Speed reduction on approach
            if (currentAlt < 3000) {
                newSpeed = Math.max(FINAL_APPROACH_SPEED_KTS, currentSpeed - 2);
            } else if (currentAlt < 10000) {
                newSpeed = Math.max(APPROACH_SPEED_KTS, currentSpeed - 1);
            } else {
                newSpeed = currentSpeed - 0.5;
            }
        } else {
            // Climbing profile
            double climbRate = 1500 + random.nextDouble() * 500; // ft per update
            newAlt = Math.min(CRUISE_ALTITUDE_FT, currentAlt + climbRate * timeStepHours * 60);
            
            // Speed increase on departure
            if (currentAlt < 10000) {
                newSpeed = Math.min(250, currentSpeed + 2);
            } else {
                newSpeed = Math.min(CRUISE_SPEED_KTS, currentSpeed + 1);
            }
        }

        return new Position(newPos[0], newPos[1], newAlt, newSpeed, normalizeHeading(newHeading));
    }

    /**
     * Calculate destination point given start, bearing, and distance.
     */
    private double[] calculateDestinationPoint(double lat, double lon, double bearing, double distanceKm) {
        double R = 6371.0; // Earth's radius in km
        double d = distanceKm / R;
        double brng = Math.toRadians(bearing);
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d) +
                Math.cos(lat1) * Math.sin(d) * Math.cos(brng));
        double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(d) * Math.cos(lat1),
                Math.cos(d) - Math.sin(lat1) * Math.sin(lat2));

        return new double[]{Math.toDegrees(lat2), Math.toDegrees(lon2)};
    }

    /**
     * Calculate bearing from point 1 to point 2.
     */
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    private double normalizeHeading(double heading) {
        while (heading < 0) heading += 360;
        while (heading >= 360) heading -= 360;
        return heading;
    }

    /**
     * Flight position record.
     */
    public record Position(double latitude, double longitude, double altitude, double speed, double heading) {}
}
