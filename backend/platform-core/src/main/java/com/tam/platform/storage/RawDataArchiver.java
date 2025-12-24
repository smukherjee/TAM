package com.tam.platform.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tam.platform.event.DomainEvent;
import com.tam.platform.event.DomainSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Consumes all domain events and archives the raw event data to object storage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RawDataArchiver {

    private final ObjectStorageService objectStorageService;
    private final ObjectMapper objectMapper;

    private static final String BUCKET_PREFIX = "tenant-";
    private static final String BUCKET_SUFFIX = "-archive";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm");

    @DomainSubscriber(topics = "#")
    public void archive(DomainEvent<?> event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            String bucket = generateBucket(event);
            String key = generateKey(event);

            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

            objectStorageService.upload(bucket, key, inputStream, data.length, "application/json");

            log.debug("Archived event {} to {}/{}", event.eventId(), bucket, key);
        } catch (Exception e) {
            log.error("Failed to archive event {}", event.eventId(), e);
        }
    }

    private String generateBucket(DomainEvent<?> event) {
        return BUCKET_PREFIX + event.context().tenantId() + BUCKET_SUFFIX;
    }

    private String generateKey(DomainEvent<?> event) {
        String timestamp = event.timestamp().format(FORMATTER);
        return String.format("%s/%s/%s/%s.json",
                event.context().tenantId(),
                event.context().domainId(),
                timestamp,
                event.eventId());
    }
}