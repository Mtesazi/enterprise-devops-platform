package com.mtesazi.auditservice.service;

import com.mtesazi.auditservice.dto.AuditLogResponse;
import com.mtesazi.auditservice.entity.AuditLog;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;

import java.util.List;

public interface AuditService {

    AuditLogResponse handleEmployeeCreated(EmployeeCreatedEvent event);

    List<AuditLogResponse> getAllAuditLogs();

    AuditLogResponse toResponse(AuditLog auditLog);
}
