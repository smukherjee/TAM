package com.utam.turnaround.controller;

import com.utam.turnaround.domain.Alert;
import com.utam.turnaround.service.AlertService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<Alert> getActiveAlerts(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(value = "icaoCode", required = false) String icaoCodeParam) {
        String tenantCode = icaoCodeHeader != null ? icaoCodeHeader : (icaoCodeParam != null ? icaoCodeParam : "VIDP");
        return alertService.getActiveAlerts(tenantCode);
    }
}
