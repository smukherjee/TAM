package com.utam.turnaround.service;

import com.utam.turnaround.domain.Alert;
import com.utam.turnaround.repository.AlertRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> getActiveAlerts(String icaoCode) {
        return alertRepository.findByIcaoCodeAndIsActiveTrue(icaoCode);
    }
}
