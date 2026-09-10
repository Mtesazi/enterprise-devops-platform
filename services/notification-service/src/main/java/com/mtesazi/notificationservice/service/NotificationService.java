package com.mtesazi.notificationservice.service;

import com.mtesazi.notificationservice.dto.NotificationResponse;
import com.mtesazi.notificationservice.entity.Notification;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;

import java.util.List;

public interface NotificationService {

    NotificationResponse handleEmployeeCreated(EmployeeCreatedEvent event);

    List<NotificationResponse> getAllNotifications();

    NotificationResponse toResponse(Notification notification);
}
