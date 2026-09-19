package com.mtesazi.employeeservice.config;

import com.mtesazi.sharedlibrary.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Explicitly provisions the topics this service owns as a producer, instead of relying on
 * broker-side auto topic creation. Spring's {@code KafkaAdmin} auto-configuration picks up
 * {@link NewTopic} beans and creates them (idempotently) on startup.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic employeeCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.EMPLOYEE_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
