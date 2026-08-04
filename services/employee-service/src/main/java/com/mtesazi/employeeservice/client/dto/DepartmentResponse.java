package com.mtesazi.employeeservice.client.dto;

public record DepartmentResponse(
        Long id,
        String name,
        String code,
        String description
) {
}
