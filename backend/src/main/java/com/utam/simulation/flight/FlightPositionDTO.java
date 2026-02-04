package com.utam.simulation.flight;

import lombok.Builder;
import lombok.Data;

/**
 * FlightPositionDTO for real-time flight position API responses.
 * T071: Lightweight DTO for WebSocket/SSE streaming.
 */
@Data
@Builder
public class FlightPositionDTO {
    
    private String flightNumber;
    private String callsign;
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private double heading;
    private String status;
    
    // Default constructor for Jackson
    public FlightPositionDTO() {}
    
    // All-args constructor for builder
    public FlightPositionDTO(String flightNumber, String callsign, double latitude, 
                             double longitude, double altitude, double speed, 
                             double heading, String status) {
        this.flightNumber = flightNumber;
        this.callsign = callsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.heading = heading;
        this.status = status;
    }
}
