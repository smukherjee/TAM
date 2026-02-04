package com.utam.simulation.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * VehiclePositionRepository for time-series position data.
 * T070: Provides efficient queries for position history and trails.
 */
@Repository
public interface VehiclePositionRepository extends JpaRepository<VehiclePosition, UUID> {
    
    /**
     * Find positions for a vehicle within a time range (for movement trails).
     */
    List<VehiclePosition> findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(
        UUID vehicleId, Instant start, Instant end);
    
    /**
     * Find latest position for a vehicle.
     */
    @Query("SELECT vp FROM VehiclePosition vp WHERE vp.vehicleId = :vehicleId " +
           "ORDER BY vp.recordedAt DESC LIMIT 1")
    VehiclePosition findLatestByVehicleId(UUID vehicleId);
    
    /**
     * Find latest positions for all vehicles in a tenant.
     */
    @Query(value = """
        SELECT DISTINCT ON (vehicle_id) * 
        FROM simulation_vehicle_positions 
        WHERE tenant_code = :tenantCode 
        ORDER BY vehicle_id, recorded_at DESC
        """, nativeQuery = true)
    List<VehiclePosition> findLatestByTenantCode(String tenantCode);
    
    /**
     * Find positions for a tenant within a time range.
     */
    List<VehiclePosition> findByTenantCodeAndRecordedAtBetweenOrderByRecordedAtAsc(
        String tenantCode, Instant start, Instant end);
    
    /**
     * Delete positions older than a given timestamp (for data retention).
     */
    void deleteByRecordedAtBefore(Instant before);
    
    /**
     * Count positions for a tenant (for metrics).
     */
    long countByTenantCode(String tenantCode);
}
