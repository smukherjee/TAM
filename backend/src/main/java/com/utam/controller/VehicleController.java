package com.utam.controller;

import com.utam.model.Vehicle;
import com.utam.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<Vehicle> getAllVehicles(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(value = "tenantCode", required = false) String tenantCodeParam) {
        // Prefer query param over header
        String icaoCode = tenantCodeParam != null && !tenantCodeParam.isEmpty() ? tenantCodeParam : icaoCodeHeader;
        return vehicleService.getActiveVehicles(icaoCode);
    }
}
