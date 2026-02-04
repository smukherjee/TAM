package com.utam.turnaround.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

@Service
public class MockDataGeneratorService {

    private final RestTemplate restTemplate;
    private final Random random = new Random();
    
    @SuppressWarnings("unused") // Reserved for random status selection feature
    private final List<String> statuses = Arrays.asList("SCHEDULED", "ON_BLOCK", "OFF_BLOCK", "DEPARTED");

    @Value("${nifi.ingestion.url:http://nifi:8091/contentListener}")
    private String nifiUrl;

    public MockDataGeneratorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("✅ MockDataGeneratorService initialized with NiFi URL: " + nifiUrl);
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void generateTurnaroundEvent() {
        System.out.println("🔄 Generating turnaround event...");
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", ZonedDateTime.now().toString());
        event.put("icaoCode", "VIDP"); // Default tenant
        event.put("type", "TURNAROUND_EVENT");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("flightId", "FL" + (100 + random.nextInt(900)));
        payload.put("standId", "S" + (1 + random.nextInt(20)));
        
        // Generate status with weighted probability
        // 30% SCHEDULED, 40% ON_BLOCK, 20% OFF_BLOCK, 10% DEPARTED
        double rand = random.nextDouble();
        String status;
        if (rand < 0.3) {
            status = "SCHEDULED";
        } else if (rand < 0.7) {
            status = "ON_BLOCK";
        } else if (rand < 0.9) {
            status = "OFF_BLOCK";
        } else {
            status = "DEPARTED";
        }
        
        payload.put("status", status);
        
        // Add timing information based on status
        ZonedDateTime now = ZonedDateTime.now();
        if ("SCHEDULED".equals(status)) {
            payload.put("eibt", now.plusMinutes(30 + random.nextInt(60)).toString());
            payload.put("tsat", now.plusMinutes(75 + random.nextInt(30)).toString());
        } else if ("ON_BLOCK".equals(status)) {
            payload.put("aibt", now.minusMinutes(random.nextInt(15)).toString());
            payload.put("eibt", now.minusMinutes(5 + random.nextInt(10)).toString());
            payload.put("sirt", now.plusMinutes(random.nextInt(10)).toString());
            payload.put("tsat", now.plusMinutes(35 + random.nextInt(20)).toString());
        } else if ("OFF_BLOCK".equals(status) || "DEPARTED".equals(status)) {
            payload.put("aibt", now.minusMinutes(45 + random.nextInt(30)).toString());
            payload.put("eibt", now.minusMinutes(50 + random.nextInt(20)).toString());
            payload.put("aobt", now.minusMinutes(random.nextInt(10)).toString());
            payload.put("tobt", now.minusMinutes(5 + random.nextInt(10)).toString());
        }
        
        event.put("payload", payload);

        try {
            restTemplate.postForObject(nifiUrl, event, String.class);
            System.out.println("✅ Sent turnaround event to NiFi: flightId=" + payload.get("flightId") + ", status=" + payload.get("status"));
        } catch (Exception e) {
            System.err.println("❌ Failed to send event to NiFi: " + e.getMessage());
        }
    }
}
