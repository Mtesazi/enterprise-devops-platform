package com.mtesazi.employeeservice.dto;

import com.mtesazi.employeeservice.client.dto.DepartmentResponse;

public record EmployeeDetailsResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Long departmentId,
        DepartmentResponse department
) {
}
