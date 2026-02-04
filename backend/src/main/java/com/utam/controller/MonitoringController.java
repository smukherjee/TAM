package com.utam.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utam.model.dto.PipelineStatusDto;
import com.utam.monitoring.KafkaLagMonitor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/admin")
public class MonitoringController {

    private final ObjectMapper objectMapper;
    private final KafkaLagMonitor kafkaLagMonitor;
    private final String prometheusUrl = "http://tam-prometheus:9090";
    private final HttpClient httpClient;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.utam.config.AlertConfiguration alertConfiguration;

    // Cache for previous counter values to calculate rates
    private final Map<String, CounterSnapshot> previousSnapshots = new ConcurrentHashMap<>();

    // Track last successful data timestamp per pipeline
    private final Map<String, Long> lastDataTimestamps = new ConcurrentHashMap<>();

    // Track last non-zero rate per pipeline
    private final Map<String, Double> lastNonZeroRates = new ConcurrentHashMap<>();

    public MonitoringController(ObjectMapper objectMapper, KafkaLagMonitor kafkaLagMonitor,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
            com.utam.config.AlertConfiguration alertConfiguration) {
        this.objectMapper = objectMapper;
        this.kafkaLagMonitor = kafkaLagMonitor;
        this.messagingTemplate = messagingTemplate;
        this.alertConfiguration = alertConfiguration;
        this.httpClient = HttpClient.newHttpClient();
    }

    // Push updates every 1 second
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1000)
    public void pushPipelineStatus() {
        List<PipelineStatusDto> statusList = getStatusList();
        messagingTemplate.convertAndSend("/topic/pipeline-status", statusList);
    }

    @GetMapping("/config/alerts")
    public ResponseEntity<com.utam.config.AlertConfiguration> getAlertConfig() {
        return ResponseEntity.ok(alertConfiguration);
    }

    @GetMapping("/pipeline-status")
    public ResponseEntity<List<PipelineStatusDto>> getPipelineStatus() {
        return ResponseEntity.ok(getStatusList());
    }

    @PostMapping("/config/alerts")
    public ResponseEntity<com.utam.config.AlertConfiguration> updateAlertConfig(
            @RequestBody com.utam.config.AlertConfiguration newConfig) {
        alertConfiguration.setGreenThresholdSeconds(newConfig.getGreenThresholdSeconds());
        alertConfiguration.setAmberThresholdSeconds(newConfig.getAmberThresholdSeconds());
        return ResponseEntity.ok(alertConfiguration);
    }

    @SuppressWarnings("unused") // Rate and health values are calculated for side effects (caching in maps)
    private List<PipelineStatusDto> getStatusList() {
        List<PipelineStatusDto> statusList = new ArrayList<>();
        long now = System.currentTimeMillis();

        // 1. Turnaround (VIDP)
        double vidpSourceCount = getPrometheusMetric(
                "simulator_events_generated_total{type=\"turnaround\",icao=\"VIDP\"}");
        double vidpSourceRate = calculateRate("vidp_source", vidpSourceCount, now);

        // 2. Turnaround (LIRN)
        double lirnSourceCount = getPrometheusMetric(
                "simulator_events_generated_total{type=\"turnaround\",icao=\"LIRN\"}");
        double lirnSourceRate = calculateRate("lirn_source", lirnSourceCount, now);

        // 3. Turnaround (YBBN)
        double ybbnSourceCount = getPrometheusMetric(
                "simulator_events_generated_total{type=\"turnaround\",icao=\"YBBN\"}");
        double ybbnSourceRate = calculateRate("ybbn_source", ybbnSourceCount, now);

        // 4. ADSB Flight Tracking
        double adsbSourceCount = getPrometheusMetric("simulator_events_generated_total{type=\"flight\"}");
        double adsbSourceRate = calculateRate("adsb_source", adsbSourceCount, now);

        // 4. Vehicle Tracking (NEW)
        double vehicleSourceCount = getPrometheusMetric("simulator_events_generated_total{type=\"vehicle\"}");
        double vehicleSourceRate = calculateRate("vehicle_source", vehicleSourceCount, now);

        // Sink metrics
        double turnaroundSinkCount = getPrometheusMetric("kafka_events_consumed_total{type=\"turnaround\"}");
        double turnaroundSinkRate = calculateRate("turnaround_sink", turnaroundSinkCount, now);

        // Calculate health based on last data time
        String vidpHealth = calculateHealth("vidp_source");
        String lirnHealth = calculateHealth("lirn_source");
        String ybbnHealth = calculateHealth("ybbn_source");
        String adsbHealth = calculateHealth("adsb_source");
        String vehicleHealth = calculateHealth("vehicle_source");

        // Get real Kafka lag
        long turnaroundLag = kafkaLagMonitor.getTopicLag("turnaround-events");

        // Build status list
        // Create wrapper helpers
        statusList.add(createStatus("Turnaround (VIDP)", "turnaround", "VIDP", turnaroundLag));
        statusList.add(createStatus("Turnaround (LIRN)", "turnaround", "LIRN", turnaroundLag));
        statusList.add(createStatus("Turnaround (YBBN)", "turnaround", "YBBN", turnaroundLag));
        statusList.add(createStatus("ADSB Flight Tracking", "flight", null, 0));
        statusList.add(createStatus("Vehicle Tracking", "vehicle", null, 0));

        return statusList;
    }

    private double calculateRate(String key, double currentCount, long currentTime) {
        CounterSnapshot previous = previousSnapshots.get(key);

        // If counter value changed, update last data timestamp
        if (currentCount > 0 && (previous == null || currentCount > previous.value)) {
            lastDataTimestamps.put(key, currentTime);
        }

        if (previous == null || currentCount == 0) {
            previousSnapshots.put(key, new CounterSnapshot(currentCount, currentTime));
            return 0.0;
        }

        double countDelta = currentCount - previous.value;
        long timeDelta = currentTime - previous.timestamp;

        previousSnapshots.put(key, new CounterSnapshot(currentCount, currentTime));

        if (timeDelta <= 0 || countDelta < 0) {
            return 0.0;
        }

        double rate = (countDelta / timeDelta) * 1000.0;

        // Track last non-zero rate
        if (rate > 0) {
            lastNonZeroRates.put(key, rate);
        }

        return rate;
    }

    private String calculateHealth(String pipelineKey) {
        Long lastTimestamp = lastDataTimestamps.get(pipelineKey);

        if (lastTimestamp == null) {
            return "RED"; // No data ever = Critical
        }

        long now = System.currentTimeMillis();
        long secondsSinceLastData = (now - lastTimestamp) / 1000;

        if (secondsSinceLastData <= alertConfiguration.getGreenThresholdSeconds()) {
            return "GREEN";
        } else if (secondsSinceLastData <= alertConfiguration.getAmberThresholdSeconds()) {
            return "AMBER";
        }
        return "RED";
    }

    private double getPrometheusMetric(String query) {
        try {
            String encodedQuery = query
                    .replace("\"", "%22")
                    .replace("{", "%7B")
                    .replace("}", "%7D")
                    .replace(",", "%2C")
                    .replace("=", "%3D");
            String fullUrl = prometheusUrl + "/api/v1/query?query=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.path("status").asText().equals("success")) {
                JsonNode result = root.path("data").path("result");
                if (result.isArray() && result.size() > 0) {
                    JsonNode value = result.get(0).path("value");
                    if (value.isArray() && value.size() >= 2) {
                        return value.get(1).asDouble();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying Prometheus for " + query + ": " + e.getMessage());
        }
        return 0.0;
    }

    @SuppressWarnings("unused") // avgLatency computed for reference, liveAvgLatency used in actual response
    private PipelineStatusDto createStatus(String name, String type, String icao, double lag) {
        long now = System.currentTimeMillis();

        // 1. Source Rate
        String sourceQuery = "simulator_events_generated_total{type=\"" + type + "\""
                + (icao != null ? ",icao=\"" + icao + "\"" : "") + "}";
        double sourceCount = getPrometheusMetric(sourceQuery);
        String key = type + (icao != null ? "_" + icao : "") + "_source";
        double sourceRate = calculateRate(key, sourceCount, now);

        // 2. Health
        String health = calculateHealth(key);

        // 3. Sink/Consumer Rate (using consumed_total if available)
        // Note: Currently consumed_total is only for Turnaround. We might need to make
        // it generic.
        // For now, if generic consumer metric isn't there, we use what we have or 0.
        // But we added standardized metrics: 'kafka.events.consumed' (Turnaround) or
        // just implicit save?
        // We added recording logic but maybe not a clear 'events_consumed' metric for
        // Flight/Vehicle yet?
        // Wait, TurnaroundService has 'kafka.events.consumed'.
        // Vehicle/Flight just save. They track LATENCY and ERRORS.
        // Let's rely on 'pipeline_latency_seconds_count' as a proxy for 'consumed'
        // events count?
        String pType = type; // needed because prometheus queries use type label
        double sinkCount = getPrometheusMetric("pipeline_latency_seconds_count{type=\"" + pType + "\"}");
        double sinkRate = calculateRate(type + "_sink", sinkCount, now);

        // 4. Latency (Avg)
        double latencySum = getPrometheusMetric("pipeline_latency_seconds_sum{type=\"" + pType + "\"}");
        double latencyCount = sinkCount; // Same as count above
        double avgLatency = (latencyCount > 0) ? (latencySum / latencyCount) * 1000 : 0.0; // Seconds to ms
        // Note: Latency Summary in Prometheus accumulates. To get instantaneous avg, we
        // should diff snapshots?
        // Or just show overall avg? For live dashboard, instantaneous
        // (rate(sum)/rate(count)) is better.
        // Using rate() in PromQL is better: rate(sum[1m]) / rate(count[1m])
        // MonitoringController does raw queries. Let's do raw avg for now or implement
        // diff logic.
        // For simplicity, let's use the rate calculation logic we already have for
        // counts!
        // We can track 'latency_sum' rate and 'latency_count' rate.
        double latencySumRate = calculateRate(type + "_latscan_sum", latencySum, now);
        double latencyCountRate = calculateRate(type + "_latscan_count", latencyCount, now);
        double liveAvgLatency = (latencyCountRate > 0) ? (latencySumRate / latencyCountRate) * 1000 : 0.0;

        // 5. Error Rate
        double errorCount = getPrometheusMetric("pipeline_events_failed_total{type=\"" + pType + "\"}");
        double errorRate = calculateRate(type + "_errors", errorCount, now);

        return new PipelineStatusDto(
                name,
                sourceRate,
                sinkRate,
                lag,
                lastDataTimestamps.get(key),
                health,
                lastNonZeroRates.get(key),
                liveAvgLatency,
                errorRate);
    }

    private static class CounterSnapshot {
        final double value;
        final long timestamp;

        CounterSnapshot(double value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
