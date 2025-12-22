package com.tam.platform.event;

import org.springframework.core.annotation.AliasFor;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a domain event subscriber.
 * Meta-annotation for @KafkaListener.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@KafkaListener
public @interface DomainSubscriber {

    @AliasFor(annotation = KafkaListener.class, attribute = "topics")
    String[] topics() default {};

    @AliasFor(annotation = KafkaListener.class, attribute = "groupId")
    String groupId() default "";

    @AliasFor(annotation = KafkaListener.class, attribute = "containerFactory")
    String containerFactory() default "domainKafkaListenerContainerFactory";
}
