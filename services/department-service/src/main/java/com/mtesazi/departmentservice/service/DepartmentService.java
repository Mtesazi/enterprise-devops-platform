package com.mtesazi.departmentservice.service;

import com.mtesazi.departmentservice.dto.DepartmentRequest;
import com.mtesazi.departmentservice.dto.DepartmentResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(@Valid DepartmentRequest request);

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse updateDepartment(Long id, @Valid DepartmentRequest request);

    void deleteDepartment(Long id);
}
