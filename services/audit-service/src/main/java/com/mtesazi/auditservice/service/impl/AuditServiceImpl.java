package com.mtesazi.auditservice.service.impl;

import com.mtesazi.auditservice.dto.AuditLogResponse;
import com.mtesazi.auditservice.entity.AuditLog;
import com.mtesazi.auditservice.repository.AuditLogRepository;
import com.mtesazi.auditservice.service.AuditService;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLogResponse handleEmployeeCreated(EmployeeCreatedEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEventType("EMPLOYEE_CREATED");
        auditLog.setEmployeeId(event.employeeId());
        auditLog.setDetails("Employee " + event.firstName() + " " + event.lastName() + " was created");
        return toResponse(auditLogRepository.save(auditLog));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllAuditLogs() {
        return auditLogRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AuditLogResponse toResponse(AuditLog auditLog) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(auditLog.getId());
        response.setEventType(auditLog.getEventType());
        response.setEmployeeId(auditLog.getEmployeeId());
        response.setDetails(auditLog.getDetails());
        response.setCreatedAt(auditLog.getCreatedAt());
        return response;
    }
}
