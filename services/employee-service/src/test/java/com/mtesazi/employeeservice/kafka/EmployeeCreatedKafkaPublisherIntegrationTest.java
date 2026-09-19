package com.mtesazi.employeeservice.kafka;

import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import com.mtesazi.sharedlibrary.kafka.KafkaTopics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link EmployeeCreatedKafkaPublisher} serializes and sends
 * {@link EmployeeCreatedEvent}s to the real (embedded) {@code employee.created} topic,
 * which the {@link com.mtesazi.employeeservice.config.KafkaTopicConfig} bean provisions on
 * startup.
 *
 * <p>The {@code @TransactionalEventListener(phase = AFTER_COMMIT)} dispatch from
 * {@code EmployeeServiceImpl} into this publisher is already covered by
 * {@code EmployeeServiceImplTest} (Mockito verification that {@code publishEvent} is called).
 * This test instead exercises the publisher's own send path against a real broker.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.main.lazy-initialization=true",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = KafkaTopics.EMPLOYEE_CREATED)
class EmployeeCreatedKafkaPublisherIntegrationTest {

    @Autowired
    private EmployeeCreatedKafkaPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, EmployeeCreatedEvent> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publishesEmployeeCreatedEventToTheProvisionedTopic() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "employee-created-test-consumer", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.mtesazi.sharedlibrary.kafka");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmployeeCreatedEvent.class.getName());
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.EMPLOYEE_CREATED);

        EmployeeCreatedEvent event = new EmployeeCreatedEvent(
                42L, "Ada", "Lovelace", "ada@example.com", "Engineering",
                BigDecimal.valueOf(1000), LocalDateTime.now());

        publisher.publish(event);

        ConsumerRecord<String, EmployeeCreatedEvent> record =
                KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.EMPLOYEE_CREATED, Duration.ofSeconds(10));

        assertThat(record.key()).isEqualTo("42");
        assertThat(record.value().employeeId()).isEqualTo(42L);
        assertThat(record.value().firstName()).isEqualTo("Ada");
        assertThat(record.value().email()).isEqualTo("ada@example.com");
    }
}
