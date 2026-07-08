package com.mtesazi.sharedlibrary.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeCreatedEvent(
        Long employeeId,
        String firstName,
        String lastName,
        String email,
        String department,
        BigDecimal salary,
        LocalDateTime createdAt
) {
}
