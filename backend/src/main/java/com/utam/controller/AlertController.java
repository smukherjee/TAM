package com.utam.controller;

import com.utam.model.Alert;
import com.utam.service.AlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<Alert> getAllAlerts(@RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        return alertService.getAllAlerts(icaoCode);
    }
}
