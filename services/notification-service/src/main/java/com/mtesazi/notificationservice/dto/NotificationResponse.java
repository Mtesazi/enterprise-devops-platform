package com.mtesazi.notificationservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private Long id;
    private Long employeeId;
    private String message;
    private LocalDateTime createdAt;
}
