package com.utam.controller;

import com.utam.model.VehicleAlert;
import com.utam.service.VehicleAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-alerts")
public class VehicleAlertController {

    private final VehicleAlertService alertService;

    public VehicleAlertController(VehicleAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<VehicleAlert> getAllAlerts(@RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        return alertService.getAllAlerts(icaoCode);
    }
}
