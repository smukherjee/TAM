package com.utam.simulation;

import com.utam.model.dto.CvEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockCvEventGenerator_LIRN {

    private static final Logger log = LoggerFactory.getLogger(MockCvEventGenerator_LIRN.class);
    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter eventCounter;

    @Value("${simulation.cv-url}")
    private String ingestionUrl;

    // Stand -> (Activity -> StartTime)
    private final Map<String, Map<String, LocalDateTime>> standActivityState = new ConcurrentHashMap<>();

    private final List<String> activityTypes = Arrays.asList(
            "Passenger Boarding Bridge",
            "Passenger Step Ladder Front",
            "Fuel Vehicle",
            "Push Back Tug",
            "Bag Movement Arrival");

    private final List<String> stands = Arrays.asList("NAP_1", "NAP_2", "NAP_3");

    public MockCvEventGenerator_LIRN(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.eventCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "turnaround")
                .tag("icao", "LIRN")
                .description("Number of mock LIRN turnaround events generated")
                .register(registry);
    }

    @Scheduled(fixedRate = 2000)
    public void generateCvEvent() {
        for (String stand : stands) {
            processStand(stand);
        }
    }

    private void processStand(String stand) {
        String icao = "LIRN";
        Map<String, LocalDateTime> activeActivities = standActivityState.computeIfAbsent(stand, k -> new HashMap<>());
        List<CvEventDto> eventsToSend = new ArrayList<>();

        // Stop logic
        Iterator<Map.Entry<String, LocalDateTime>> iterator = activeActivities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            if (LocalDateTime.now().minusSeconds(10).isAfter(entry.getValue()) && random.nextDouble() < 0.2) {
                eventsToSend.add(createEvent(stand, entry.getKey(), 1, icao));
                iterator.remove();
            }
        }

        // Start logic
        if (activeActivities.size() < 2 && random.nextDouble() < 0.3) {
            String newActivity = activityTypes.get(random.nextInt(activityTypes.size()));
            if (!activeActivities.containsKey(newActivity)) {
                eventsToSend.add(createEvent(stand, newActivity, 0, icao));
                activeActivities.put(newActivity, LocalDateTime.now());
            }
        }

        for (CvEventDto dto : eventsToSend) {
            sendEvent(dto);
        }
    }

    private CvEventDto createEvent(String stand, String activityType, int eventType, String icao) {
        CvEventDto dto = new CvEventDto();
        dto.setIcaoCode(icao);
        dto.setEventUniqueId(UUID.randomUUID().toString());
        dto.setCameraId("NAP-CAM-" + stand);
        dto.setCameraName("Naples Cam " + stand);
        dto.setActivityType(activityType);
        dto.setEventType(eventType);
        dto.setEventTimeStamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setStand(stand);
        dto.setCreationTimestamp(System.currentTimeMillis());
        return dto;
    }

    private void sendEvent(CvEventDto dto) {
        try {
            log.info("Generated LIRN CV event: {} ({}) at {}", dto.getActivityType(),
                    dto.getEventType() == 0 ? "START" : "STOP", dto.getStand());
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(dto), Void.class);
            eventCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send mock LIRN CV event: {}", e.getMessage());
        }
    }
}
