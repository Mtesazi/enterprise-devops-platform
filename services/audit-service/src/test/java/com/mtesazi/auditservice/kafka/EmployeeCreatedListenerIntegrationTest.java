package com.mtesazi.auditservice.kafka;

import com.mtesazi.auditservice.entity.AuditLog;
import com.mtesazi.auditservice.repository.AuditLogRepository;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import com.mtesazi.sharedlibrary.kafka.KafkaTopics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of the {@code employee.created} consumption flow in this service: a real
 * message is produced to an embedded broker, consumed by {@link EmployeeCreatedListener}, and
 * handled by {@code AuditServiceImpl}.
 *
 * <p>This service wires the same retry + dead-letter-topic policy as notification-service
 * ({@code com.mtesazi.auditservice.config.KafkaConsumerErrorHandlingConfig}); that behavior is
 * verified once, end-to-end, in {@code notification-service}'s
 * {@code EmployeeCreatedListenerIntegrationTest} rather than duplicated here.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=audit-service-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.mtesazi.sharedlibrary.kafka",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent",
        "spring.kafka.consumer.properties.spring.json.use.type.headers=false",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopics.EMPLOYEE_CREATED,
        KafkaTopics.EMPLOYEE_CREATED + ".DLT"
})
class EmployeeCreatedListenerIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> producerTemplate;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    void consumesEmployeeCreatedEventAndPersistsAnAuditLog() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> {
                    AuditLog auditLog = invocation.getArgument(0);
                    auditLog.setId(1L);
                    auditLog.setCreatedAt(LocalDateTime.now());
                    return auditLog;
                });

        EmployeeCreatedEvent event = new EmployeeCreatedEvent(
                7L, "Grace", "Hopper", "grace@example.com", "Engineering",
                BigDecimal.valueOf(2000), LocalDateTime.now());

        producerTemplate.send(KafkaTopics.EMPLOYEE_CREATED, event.employeeId().toString(), event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(auditLogRepository, times(1)).save(any(AuditLog.class)));
    }
}
