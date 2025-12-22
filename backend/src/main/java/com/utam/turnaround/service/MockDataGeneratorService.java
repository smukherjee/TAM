package com.utam.turnaround.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.time.ZonedDateTime;
import java.util.Random;

@Service
public class MockDataGeneratorService {

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${nifi.ingestion.url:http://nifi:8091/contentListener}")
    private String nifiUrl;

    public MockDataGeneratorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void generateTurnaroundEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", ZonedDateTime.now().toString());
        event.put("icaoCode", "VIDP"); // Default tenant
        event.put("type", "TURNAROUND_EVENT");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("flightId", "FL" + (100 + random.nextInt(900)));
        payload.put("standId", "S" + (1 + random.nextInt(20)));
        payload.put("status", random.nextBoolean() ? "ON_BLOCK" : "OFF_BLOCK");
        
        event.put("payload", payload);

        try {
            restTemplate.postForObject(nifiUrl, event, String.class);
            // System.out.println("Sent mock event to NiFi: " + event);
        } catch (Exception e) {
            // System.err.println("Failed to send event to NiFi: " + e.getMessage());
            // Suppress errors for now as NiFi might not be running in dev env always
        }
    }
}
