package com.utam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlertConfiguration {

    private int greenThresholdSeconds;
    private int amberThresholdSeconds;

    public AlertConfiguration(
            @Value("${monitoring.health.green-threshold-seconds:60}") int greenThresholdSeconds,
            @Value("${monitoring.health.amber-threshold-seconds:120}") int amberThresholdSeconds) {
        this.greenThresholdSeconds = greenThresholdSeconds;
        this.amberThresholdSeconds = amberThresholdSeconds;
    }

    public int getGreenThresholdSeconds() {
        return greenThresholdSeconds;
    }

    public void setGreenThresholdSeconds(int greenThresholdSeconds) {
        this.greenThresholdSeconds = greenThresholdSeconds;
    }

    public int getAmberThresholdSeconds() {
        return amberThresholdSeconds;
    }

    public void setAmberThresholdSeconds(int amberThresholdSeconds) {
        this.amberThresholdSeconds = amberThresholdSeconds;
    }
}
