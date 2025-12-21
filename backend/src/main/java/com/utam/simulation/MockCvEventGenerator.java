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
public class MockCvEventGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockCvEventGenerator.class);
    private final RestTemplate restTemplate;
    private final Random random = new Random();
    private final io.micrometer.core.instrument.Counter eventCounter;

    @Value("${simulation.cv-url}")
    private String ingestionUrl;

    // Stand -> (Activity -> StartTime)
    private final Map<String, Map<String, LocalDateTime>> standActivityState = new ConcurrentHashMap<>();

    private final List<String> activityTypes = Arrays.asList("Passenger Boarding Bridge", "Passenger Step Ladder Front",
            "Passenger Step Ladder Back", "Towable Conveyor Belt Front", "Towable Conveyor Belt Back",
            "Aircraft Back Door", "Aircraft Front Door", "Aircraft Front Belly", "Aircraft Back Belly",
            "Passenger Coach", "Person Arrival Movement", "Person Departure Movement",
            "Head Unit (EBT) / Diesel Tug (DT)", "Bag Movement Arrival", "Bag Movement Departure", "Fuel Vehicle",
            "Water Cart", "Toilet Cart", "Ambulance", "Push Back Tug");

    private final List<String> stands = Arrays.asList("D7", "B106", "C005");

    public MockCvEventGenerator(RestTemplate restTemplate, io.micrometer.core.instrument.MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.eventCounter = io.micrometer.core.instrument.Counter.builder("simulator.events.generated")
                .tag("type", "turnaround")
                .tag("icao", "VIDP")
                .description("Number of mock turnaround events generated")
                .register(registry);
    }

    @Scheduled(fixedRate = 2000) // Check every 2 seconds
    public void generateCvEvent() {
        for (String stand : stands) {
            processStand(stand);
        }
    }

    private void processStand(String stand) {
        String icao = "VIDP";
        Map<String, LocalDateTime> activeActivities = standActivityState.computeIfAbsent(stand, k -> new HashMap<>());
        List<CvEventDto> eventsToSend = new ArrayList<>();

        // 1. Try to STOP active activities
        Iterator<Map.Entry<String, LocalDateTime>> iterator = activeActivities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            String activity = entry.getKey();
            LocalDateTime startTime = entry.getValue();

            // Min duration 10s, Max duration random chance
            if (LocalDateTime.now().minusSeconds(10).isAfter(startTime)) {
                // 20% chance to stop every tick after 10s
                if (random.nextDouble() < 0.2) {
                    eventsToSend.add(createEvent(stand, activity, 1, icao)); // STOP
                    iterator.remove();
                }
            }
        }

        // 2. Try to START new activities (max 3 concurrent)
        if (activeActivities.size() < 3) {
            // 30% chance to start a new one
            if (random.nextDouble() < 0.3) {
                String newActivity = pickRandomInactiveActivity(activeActivities.keySet());
                if (newActivity != null) {
                    eventsToSend.add(createEvent(stand, newActivity, 0, icao)); // START
                    activeActivities.put(newActivity, LocalDateTime.now());
                }
            }
        }

        // Send events
        for (CvEventDto dto : eventsToSend) {
            sendEvent(dto);
        }
    }

    private String pickRandomInactiveActivity(Set<String> activeTypes) {
        List<String> candidates = new ArrayList<>(activityTypes);
        candidates.removeAll(activeTypes);
        if (candidates.isEmpty())
            return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private CvEventDto createEvent(String stand, String activityType, int eventType, String icao) {
        CvEventDto dto = new CvEventDto();
        dto.setIcaoCode(icao);
        dto.setEventUniqueId(UUID.randomUUID().toString());
        dto.setCameraId(String.valueOf(1 + random.nextInt(10)));
        dto.setCameraName("Cam-" + dto.getCameraId());

        // Randomly format to test normalization (snake_case vs Title Case)
        if (random.nextBoolean()) {
            dto.setActivityType(
                    activityType.toLowerCase().replace(" ", "_").replace("/", "").replace("(", "").replace(")", ""));
        } else {
            dto.setActivityType(activityType);
        }

        dto.setEventType(eventType);
        dto.setEventTimeStamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setStand(stand);
        dto.setCreationTimestamp(System.currentTimeMillis());
        return dto;
    }

    private void sendEvent(CvEventDto dto) {
        try {
            log.info("Generated CV event: {} ({}) at {}", dto.getActivityType(),
                    dto.getEventType() == 0 ? "START" : "STOP", dto.getStand());
            restTemplate.postForObject(ingestionUrl, Collections.singletonList(dto), Void.class);
            eventCounter.increment();
        } catch (Exception e) {
            log.error("Failed to send mock CV event: {}", e.getMessage());
        }
    }
}
