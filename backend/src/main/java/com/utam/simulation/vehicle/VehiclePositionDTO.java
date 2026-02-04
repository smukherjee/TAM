package com.utam.simulation.vehicle;

import lombok.Builder;
import lombok.Data;

/**
 * VehiclePositionDTO for real-time position API responses.
 * T070: Lightweight DTO for WebSocket/SSE streaming.
 */
@Data
@Builder
public class VehiclePositionDTO {
    
    private String vehicleId;
    private double latitude;
    private double longitude;
    private double speed;
    private double heading;
    private String status;
    
    // Default constructor for Jackson
    public VehiclePositionDTO() {}
    
    // All-args constructor for builder
    public VehiclePositionDTO(String vehicleId, double latitude, double longitude, 
                              double speed, double heading, String status) {
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.status = status;
    }
}
