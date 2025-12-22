package com.tam.platform.event;

import com.tam.platform.context.TenantContext;
import com.tam.platform.context.TenantContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class KafkaEventBus implements EventBus {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String eventType, Object payload) {
        TenantContext context = TenantContextService.get();
        if (context == null) {
            context = TenantContext.empty();
        }

        DomainEvent<Object> event = new DomainEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                context,
                payload
        );

        List<Header> headers = new ArrayList<>();
        if (context.correlationId() != null) {
            headers.add(new RecordHeader("X-Correlation-ID", context.correlationId().getBytes(StandardCharsets.UTF_8)));
        }
        if (context.tenantId() != null) {
            headers.add(new RecordHeader("X-Tenant-ID", context.tenantId().getBytes(StandardCharsets.UTF_8)));
        }
        if (context.domainId() != null) {
            headers.add(new RecordHeader("X-Domain-ID", context.domainId().getBytes(StandardCharsets.UTF_8)));
        }
        headers.add(new RecordHeader("X-Event-Type", eventType.getBytes(StandardCharsets.UTF_8)));

        String key = context.tenantId() != null ? context.tenantId() : event.eventId();
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, null, key, event, headers);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendToKafka(record, event.eventId(), topic);
                }
            });
        } else {
            sendToKafka(record, event.eventId(), topic);
        }
    }

    private void sendToKafka(ProducerRecord<String, Object> record, String eventId, String topic) {
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event {} to topic {}", eventId, topic, ex);
            } else {
                log.debug("Published event {} to topic {}", eventId, topic);
            }
        });
    }
}
