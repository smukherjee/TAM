package com.utam.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic flightTopic() {
        return TopicBuilder.name("flight-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic vehicleTopic() {
        return TopicBuilder.name("vehicle-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic alertTopic() {
        return TopicBuilder.name("alert-events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
