package com.mtesazi.notificationservice.kafka;

import com.mtesazi.notificationservice.service.NotificationService;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import com.mtesazi.sharedlibrary.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeCreatedListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.EMPLOYEE_CREATED)
    public void handle(EmployeeCreatedEvent event) {
        notificationService.handleEmployeeCreated(event);
    }
}
