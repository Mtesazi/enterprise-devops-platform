package com.mtesazi.auditservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Retry and dead-letter policy for {@code @KafkaListener} consumers in this service.
 *
 * <p>A failing listener invocation is retried with a fixed backoff. Once retries are
 * exhausted, the offending record is published to a {@code <topic>.DLT} topic (partitioned
 * the same way as the source record) instead of blocking the partition or being silently
 * dropped, and the original consumer offset is committed so processing can continue.
 */
@Configuration
public class KafkaConsumerErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaOperations<Object, Object> kafkaOperations,
            @Value("${app.kafka.consumer.retry.max-attempts:3}") long maxAttempts,
            @Value("${app.kafka.consumer.retry.backoff-interval-ms:1000}") long backoffIntervalMs) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // FixedBackOff's max attempts counts retries after the first (failed) delivery attempt.
        FixedBackOff backOff = new FixedBackOff(backoffIntervalMs, Math.max(0, maxAttempts - 1));

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
