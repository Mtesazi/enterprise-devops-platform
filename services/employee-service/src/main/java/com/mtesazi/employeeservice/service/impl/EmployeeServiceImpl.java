package com.mtesazi.employeeservice.service.impl;

import com.mtesazi.employeeservice.dto.EmployeeRequest;
import com.mtesazi.employeeservice.dto.EmployeeResponse;
import com.mtesazi.employeeservice.entity.Employee;
import com.mtesazi.employeeservice.exception.EmployeeNotFoundException;
import com.mtesazi.employeeservice.integration.DepartmentServiceClient;
import com.mtesazi.employeeservice.mapper.EmployeeMapper;
import com.mtesazi.employeeservice.repository.EmployeeRepository;
import com.mtesazi.employeeservice.service.EmployeeService;
import com.mtesazi.sharedlibrary.kafka.EmployeeCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final DepartmentServiceClient departmentServiceClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        departmentServiceClient.validateDepartmentExists(request.getDepartment());
        Employee employee = employeeMapper.toEntity(request);
        Employee savedEmployee = employeeRepository.save(employee);
        applicationEventPublisher.publishEvent(new EmployeeCreatedEvent(
                savedEmployee.getId(),
                savedEmployee.getFirstName(),
                savedEmployee.getLastName(),
                savedEmployee.getEmail(),
                savedEmployee.getDepartment(),
                savedEmployee.getSalary(),
                savedEmployee.getCreatedAt()
        ));
        return employeeMapper.toResponse(savedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        return employeeMapper.toResponse(findEmployeeOrThrow(id));
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        departmentServiceClient.validateDepartmentExists(request.getDepartment());
        Employee employee = findEmployeeOrThrow(id);
        employeeMapper.applyRequestToEntity(request, employee);
        Employee updatedEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(updatedEmployee);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        employeeRepository.delete(employee);
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee " + id + " not found"));
    }
}
