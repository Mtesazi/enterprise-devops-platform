package com.mtesazi.notificationservice.service.impl;

import com.mtesazi.notificationservice.dto.NotificationResponse;
import com.mtesazi.notificationservice.entity.Notification;
import com.mtesazi.notificationservice.repository.NotificationRepository;
import com.mtesazi.notificationservice.service.NotificationService;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse handleEmployeeCreated(EmployeeCreatedEvent event) {
        Notification notification = new Notification();
        notification.setEmployeeId(event.employeeId());
        notification.setMessage("Employee " + event.firstName() + " " + event.lastName() + " was created");
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setEmployeeId(notification.getEmployeeId());
        response.setMessage(notification.getMessage());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
