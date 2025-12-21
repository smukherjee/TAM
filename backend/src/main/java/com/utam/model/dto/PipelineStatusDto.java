package com.utam.model.dto;

import lombok.Data;

@Data
public class PipelineStatusDto {
    private String flowName;
    private double sourceRate;
    private double sinkRate;
    private double lag;
    private String health;
    private Long lastDataTimestamp; // Epoch millis when last data was received
    private String timeSinceLastData; // Human-readable (e.g., "30s ago")
    private Double lastNonZeroRate; // Value of rate when it was last > 0
    private double avgLatency; // ms
    private double errorRate; // errors/sec

    public PipelineStatusDto(String flowName, double sourceRate, double sinkRate, double lag,
            Long lastDataTimestamp, String health, Double lastNonZeroRate, double avgLatency, double errorRate) {
        this.flowName = flowName;
        this.sourceRate = sourceRate;
        this.sinkRate = sinkRate;
        this.lag = lag;
        this.lastDataTimestamp = lastDataTimestamp;
        this.health = health;
        this.timeSinceLastData = formatTimeSince(lastDataTimestamp);
        this.lastNonZeroRate = lastNonZeroRate;
        this.avgLatency = avgLatency;
        this.errorRate = errorRate;
    }

    private String formatTimeSince(Long timestamp) {
        if (timestamp == null)
            return "Never";
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60)
            return seconds + "s ago";
        if (seconds < 3600)
            return (seconds / 60) + "m ago";
        return (seconds / 3600) + "h ago";
    }
}
