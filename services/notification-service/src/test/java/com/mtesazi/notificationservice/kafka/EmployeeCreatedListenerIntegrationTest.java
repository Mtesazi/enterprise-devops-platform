package com.mtesazi.notificationservice.kafka;

import com.mtesazi.notificationservice.entity.Notification;
import com.mtesazi.notificationservice.repository.NotificationRepository;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of the {@code employee.created} consumption flow in this service: a real
 * message is produced to an embedded broker, consumed by {@link EmployeeCreatedListener}, and
 * handled by {@code NotificationServiceImpl}.
 *
 * <p>{@link #retriesThenPublishesToDeadLetterTopicOnPersistentFailure()} additionally proves out
 * the retry + dead-letter-topic policy configured in
 * {@code com.mtesazi.notificationservice.config.KafkaConsumerErrorHandlingConfig}. Since
 * audit-service wires the identical {@code DefaultErrorHandler}/{@code
 * DeadLetterPublishingRecoverer} pattern, that behavior is not re-verified there.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=notification-service-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.mtesazi.sharedlibrary.kafka",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent",
        "spring.kafka.consumer.properties.spring.json.use.type.headers=false",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "app.kafka.consumer.retry.max-attempts=2",
        "app.kafka.consumer.retry.backoff-interval-ms=50",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopics.EMPLOYEE_CREATED,
        KafkaTopics.EMPLOYEE_CREATED + ".DLT"
})
class EmployeeCreatedListenerIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> producerTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private NotificationRepository notificationRepository;

    private Consumer<String, EmployeeCreatedEvent> dltConsumer;

    @AfterEach
    void tearDown() {
        if (dltConsumer != null) {
            dltConsumer.close();
        }
    }

    @Test
    void consumesEmployeeCreatedEventAndPersistsANotification() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    notification.setId(1L);
                    notification.setCreatedAt(LocalDateTime.now());
                    return notification;
                });

        EmployeeCreatedEvent event = new EmployeeCreatedEvent(
                7L, "Grace", "Hopper", "grace@example.com", "Engineering",
                BigDecimal.valueOf(2000), LocalDateTime.now());

        producerTemplate.send(KafkaTopics.EMPLOYEE_CREATED, event.employeeId().toString(), event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(notificationRepository, times(1)).save(any(Notification.class)));
    }

    @Test
    void retriesThenPublishesToDeadLetterTopicOnPersistentFailure() {
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("simulated persistence failure"));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "employee-created-dlt-test-consumer", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.mtesazi.sharedlibrary.kafka");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmployeeCreatedEvent.class.getName());
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        dltConsumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        String dltTopic = KafkaTopics.EMPLOYEE_CREATED + ".DLT";
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(dltConsumer, dltTopic);

        EmployeeCreatedEvent event = new EmployeeCreatedEvent(
                99L, "Poison", "Pill", "poison@example.com", "Engineering",
                BigDecimal.valueOf(3000), LocalDateTime.now());

        producerTemplate.send(KafkaTopics.EMPLOYEE_CREATED, event.employeeId().toString(), event);

        // max-attempts=2 -> 1 initial delivery + 1 retry, then the recoverer publishes to the DLT.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(notificationRepository, times(2)).save(any(Notification.class)));

        ConsumerRecord<String, EmployeeCreatedEvent> dltRecord =
                KafkaTestUtils.getSingleRecord(dltConsumer, dltTopic, Duration.ofSeconds(10));

        assertThat(dltRecord.value().employeeId()).isEqualTo(99L);
    }
}
