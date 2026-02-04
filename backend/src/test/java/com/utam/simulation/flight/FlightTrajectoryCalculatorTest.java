package com.utam.simulation.flight;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FlightTrajectoryCalculator.
 */
class FlightTrajectoryCalculatorTest {

    private FlightTrajectoryCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FlightTrajectoryCalculator();
    }

    @Test
    @DisplayName("Should calculate arriving flight start position away from airport")
    void shouldCalculateArrivingFlightStartPosition() {
        double airportLat = 28.5665;
        double airportLon = 77.1031;

        FlightTrajectoryCalculator.Position startPos = calculator.calculateStartPosition(
                airportLat, airportLon, FlightDataGenerator.FlightType.ARRIVING);

        // Arriving flights should start at altitude ~10000-12000 ft
        assertTrue(startPos.altitude() >= 10000 && startPos.altitude() <= 12000);
        
        // Should have approach speed ~180-230 kts
        assertTrue(startPos.speed() >= 180 && startPos.speed() <= 230);
        
        // Should be some distance from airport
        double distance = calculateDistance(airportLat, airportLon, startPos.latitude(), startPos.longitude());
        assertTrue(distance > 30); // At least 30km away
    }

    @Test
    @DisplayName("Should calculate departing flight start position near airport")
    void shouldCalculateDepartingFlightStartPosition() {
        double airportLat = 28.5665;
        double airportLon = 77.1031;

        FlightTrajectoryCalculator.Position startPos = calculator.calculateStartPosition(
                airportLat, airportLon, FlightDataGenerator.FlightType.DEPARTING);

        // Departing flights should start at low altitude
        assertTrue(startPos.altitude() < 1500);
        
        // Should have initial departure speed
        assertTrue(startPos.speed() >= 150 && startPos.speed() <= 250);
        
        // Should be near the airport
        double distance = calculateDistance(airportLat, airportLon, startPos.latitude(), startPos.longitude());
        assertTrue(distance < 2); // Within 2km
    }

    @Test
    @DisplayName("Should descend altitude for arriving flights")
    void shouldDescendAltitudeForArrivingFlights() {
        FlightTrajectoryCalculator.Position newPos = calculator.calculateNextPosition(
                28.5, 77.1, 5000, 180, 90, FlightDataGenerator.FlightType.ARRIVING);

        // Altitude should decrease for arriving flights
        assertTrue(newPos.altitude() <= 5000);
    }

    @Test
    @DisplayName("Should climb altitude for departing flights")
    void shouldClimbAltitudeForDepartingFlights() {
        FlightTrajectoryCalculator.Position newPos = calculator.calculateNextPosition(
                28.5, 77.1, 5000, 250, 270, FlightDataGenerator.FlightType.DEPARTING);

        // Altitude should increase for departing flights
        assertTrue(newPos.altitude() >= 5000);
    }

    @Test
    @DisplayName("Should reduce speed on final approach")
    void shouldReduceSpeedOnFinalApproach() {
        FlightTrajectoryCalculator.Position newPos = calculator.calculateNextPosition(
                28.5, 77.1, 2000, 160, 90, FlightDataGenerator.FlightType.ARRIVING);

        // Speed should decrease or stay low on final approach
        assertTrue(newPos.speed() <= 165);
    }

    @Test
    @DisplayName("Should maintain heading consistency")
    void shouldMaintainHeadingConsistency() {
        double initialHeading = 90;
        FlightTrajectoryCalculator.Position newPos = calculator.calculateNextPosition(
                28.5, 77.1, 5000, 200, initialHeading, FlightDataGenerator.FlightType.ARRIVING);

        // Heading should only vary slightly
        assertTrue(Math.abs(newPos.heading() - initialHeading) < 5);
    }

    @Test
    @DisplayName("Should normalize heading to 0-360 range")
    void shouldNormalizeHeading() {
        FlightTrajectoryCalculator.Position newPos = calculator.calculateNextPosition(
                28.5, 77.1, 5000, 200, 358, FlightDataGenerator.FlightType.ARRIVING);

        // Heading should be in valid range
        assertTrue(newPos.heading() >= 0 && newPos.heading() < 360);
    }

    @Test
    @DisplayName("Should cap departing flight at cruise altitude")
    void shouldCapDepartingFlightAtCruiseAltitude() {
        FlightTrajectoryCalculator.Position newPos = calculator.calculateNextPosition(
                28.5, 77.1, 34500, 450, 270, FlightDataGenerator.FlightType.DEPARTING);

        // Should not exceed cruise altitude
        assertTrue(newPos.altitude() <= 35000);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
