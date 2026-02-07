package com.utam.asset.controller;

import com.utam.asset.dto.AssetStatusDTO;
import com.utam.asset.service.VehicleAssetMapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for vehicle-asset mapping operations.
 * Provides endpoints to query asset information linked to vehicles.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleAssetMapController {

    private final VehicleAssetMapService vehicleAssetMapService;

    public VehicleAssetMapController(VehicleAssetMapService vehicleAssetMapService) {
        this.vehicleAssetMapService = vehicleAssetMapService;
    }

    /**
     * Get asset status for a vehicle.
     * Used by map popup to display consistent status with assets table.
     *
     * @param vehicleId  Vehicle identifier
     * @param tenantCode Tenant code from header
     * @return Asset status information or 404 if not found
     */
    @GetMapping("/{vehicleId}/asset-status")
    public ResponseEntity<AssetStatusDTO> getAssetStatusByVehicleId(
            @PathVariable String vehicleId,
            @RequestHeader("X-User-ICAO") String tenantCode) {

        AssetStatusDTO status = vehicleAssetMapService.getAssetStatusByVehicleId(vehicleId, tenantCode);

        if (status == null) {
            return ResponseEntity.noContent().build(); // Return 204 instead of 404 to avoid console errors
        }

        return ResponseEntity.ok(status);
    }
}
