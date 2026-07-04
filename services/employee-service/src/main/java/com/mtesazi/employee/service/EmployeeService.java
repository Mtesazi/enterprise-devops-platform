package com.mtesazi.employee.service;

import com.mtesazi.employee.dto.EmployeeRequest;
import com.mtesazi.employee.dto.EmployeeResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse createEmployee(@Valid EmployeeRequest request);

    List<EmployeeResponse> getAllEmployees();

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse updateEmployee(Long id, @Valid EmployeeRequest request);

    void deleteEmployee(Long id);
}
