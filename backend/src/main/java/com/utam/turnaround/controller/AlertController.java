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
    public List<Alert> getActiveAlerts(@RequestParam(defaultValue = "VIDP") String icaoCode) {
        return alertService.getActiveAlerts(icaoCode);
    }
}
