package com.utam.simulation.turnaround;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Publishes test turnaround events with delays to Kafka for alert generation
 * testing.
 */
@Service
public class TurnaroundEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private final String[] stands = { "A1", "A2", "B1", "B2", "C1", "C2" };
    private final String[] statuses = { "SCHEDULED", "ON_BLOCK", "IN_PROGRESS", "COMPLETED" };

    public TurnaroundEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a test turnaround event every 30 seconds.
     * 50% chance of having a delay to trigger alert generation.
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void publishTestTurnaroundEvent() {
        try {
            // Randomly select tenant
            String[] tenants = { "VIDP", "LIRN", "YBBN" };
            String tenantCode = tenants[random.nextInt(tenants.length)];

            Map<String, Object> event = new HashMap<>();
            event.put("tenantCode", tenantCode);

            Map<String, Object> payload = new HashMap<>();
            payload.put("flightId", "FL" + (100 + random.nextInt(900)));
            payload.put("standId", stands[random.nextInt(stands.length)]);
            payload.put("status", statuses[random.nextInt(statuses.length)]);

            // 50% chance of delay
            if (random.nextBoolean()) {
                int delayMinutes = 10 + random.nextInt(40); // 10-50 minutes delay
                payload.put("delayMinutes", delayMinutes);
                payload.put("delayReason", "Operational Delay");
            } else {
                payload.put("delayMinutes", 0);
            }

            // Add timing fields
            ZonedDateTime now = ZonedDateTime.now();
            payload.put("eibt", now.plusMinutes(30).toString());
            payload.put("tsat", now.plusMinutes(75).toString());
            payload.put("tobt", now.plusMinutes(80).toString());

            event.put("payload", payload);

            kafkaTemplate.send("turnaround-events", event);

            System.out.println("📤 Published test turnaround event: " + payload.get("flightId") +
                    " [" + tenantCode + "] (delay: " + payload.get("delayMinutes") + " min)");
        } catch (Exception e) {
            System.err.println("Error publishing test turnaround event: " + e.getMessage());
        }
    }
}
