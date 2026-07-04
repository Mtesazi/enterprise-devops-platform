package com.mtesazi.employee.mapper;

import com.mtesazi.employee.dto.EmployeeRequest;
import com.mtesazi.employee.dto.EmployeeResponse;
import com.mtesazi.employee.entity.Employee;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Employee employee = new Employee();
        applyRequestToEntity(request, employee);
        return employee;
    }

    public void applyRequestToEntity(EmployeeRequest request, Employee employee) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(employee, "employee must not be null");
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setSalary(request.getSalary());
    }

    public EmployeeResponse toResponse(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setFirstName(employee.getFirstName());
        response.setLastName(employee.getLastName());
        response.setEmail(employee.getEmail());
        response.setDepartment(employee.getDepartment());
        response.setSalary(employee.getSalary());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());
        return response;
    }
}
