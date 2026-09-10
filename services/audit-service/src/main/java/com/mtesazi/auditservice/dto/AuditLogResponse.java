package com.mtesazi.auditservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {

    private Long id;
    private String eventType;
    private Long employeeId;
    private String details;
    private LocalDateTime createdAt;
}
