package com.mtesazi.departmentservice.mapper;

import com.mtesazi.departmentservice.dto.DepartmentRequest;
import com.mtesazi.departmentservice.dto.DepartmentResponse;
import com.mtesazi.departmentservice.entity.Department;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DepartmentMapper {

    public Department toEntity(DepartmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Department department = new Department();
        applyRequestToEntity(request, department);
        return department;
    }

    public void applyRequestToEntity(DepartmentRequest request, Department department) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(department, "department must not be null");
        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());
    }

    public DepartmentResponse toResponse(Department department) {
        Objects.requireNonNull(department, "department must not be null");
        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        response.setCode(department.getCode());
        response.setDescription(department.getDescription());
        response.setCreatedAt(department.getCreatedAt());
        response.setUpdatedAt(department.getUpdatedAt());
        return response;
    }
}
