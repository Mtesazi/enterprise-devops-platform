package com.mtesazi.employeeservice.kafka;

import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import com.mtesazi.sharedlibrary.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmployeeCreatedKafkaPublisher {

    private final KafkaTemplate<String, EmployeeCreatedEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EmployeeCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.EMPLOYEE_CREATED, event.employeeId().toString(), event);
    }
}
