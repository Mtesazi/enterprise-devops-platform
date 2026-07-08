package com.mtesazi.auditservice.kafka;

import com.mtesazi.auditservice.service.AuditService;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import com.mtesazi.sharedlibrary.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeCreatedListener {

    private final AuditService auditService;

    @KafkaListener(topics = KafkaTopics.EMPLOYEE_CREATED)
    public void handle(EmployeeCreatedEvent event) {
        auditService.handleEmployeeCreated(event);
    }
}
