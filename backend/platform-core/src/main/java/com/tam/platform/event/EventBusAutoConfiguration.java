package com.tam.platform.event;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@AutoConfiguration
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "platform.eventbus.kafka.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean
    public EventBus kafkaEventBus(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventBus(kafkaTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus inMemoryEventBus() {
        return new InMemoryEventBus();
    }

    @Bean
    @ConditionalOnProperty(name = "platform.eventbus.kafka.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(KafkaTemplate.class)
    public ConcurrentKafkaListenerContainerFactory<String, Object> domainKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        
        // Retry policy: Initial 1s, Multiplier 2.0, Max Interval 10s
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10000L);
        
        // DLQ Recoverer
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAdviceChain(new TenantContextAdvice());
        return factory;
    }
}
