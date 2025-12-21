package com.utam.monitoring;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaLagMonitor {

    private final AdminClient adminClient;
    private final String consumerGroupId = "utam-group";

    public KafkaLagMonitor(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    /**
     * Calculate total lag for a specific topic across all partitions
     * 
     * @param topicName the Kafka topic name
     * @return total lag (sum of all partitions)
     */
    public long getTopicLag(String topicName) {
        try {
            // Get consumer group offsets (committed positions)
            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(consumerGroupId);
            Map<TopicPartition, OffsetAndMetadata> consumerOffsets = offsetsResult.partitionsToOffsetAndMetadata()
                    .get();

            // Get topic partitions for the specified topic
            Map<TopicPartition, OffsetSpec> topicPartitions = new HashMap<>();
            for (TopicPartition tp : consumerOffsets.keySet()) {
                if (tp.topic().equals(topicName)) {
                    topicPartitions.put(tp, OffsetSpec.latest());
                }
            }

            if (topicPartitions.isEmpty()) {
                // Consumer hasn't consumed from this topic yet
                return 0;
            }

            // Get latest offsets (end of topic)
            Map<TopicPartition, Long> endOffsets = adminClient.listOffsets(topicPartitions)
                    .all()
                    .get()
                    .entrySet()
                    .stream()
                    .collect(HashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue().offset()),
                            HashMap::putAll);

            // Calculate lag
            long totalLag = 0;
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : consumerOffsets.entrySet()) {
                TopicPartition tp = entry.getKey();
                if (tp.topic().equals(topicName)) {
                    long consumerOffset = entry.getValue().offset();
                    long endOffset = endOffsets.getOrDefault(tp, 0L);
                    long lag = endOffset - consumerOffset;
                    totalLag += Math.max(0, lag);
                }
            }

            return totalLag;

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error calculating lag for topic " + topicName + ": " + e.getMessage());
            return -1; // Return -1 to indicate error
        }
    }

    /**
     * Check if consumer group exists and is active
     */
    public boolean isConsumerGroupActive() {
        try {
            adminClient.describeConsumerGroups(Collections.singleton(consumerGroupId))
                    .describedGroups()
                    .get(consumerGroupId)
                    .get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
