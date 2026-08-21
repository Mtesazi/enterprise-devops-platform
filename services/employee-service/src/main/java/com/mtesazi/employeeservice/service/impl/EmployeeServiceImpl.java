package com.mtesazi.employeeservice.service.impl;

import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.dto.EmployeeDetailsResponse;
import com.mtesazi.employeeservice.dto.EmployeeRequest;
import com.mtesazi.employeeservice.dto.EmployeeResponse;
import com.mtesazi.employeeservice.entity.Employee;
import com.mtesazi.employeeservice.exception.EmployeeNotFoundException;
import com.mtesazi.employeeservice.mapper.EmployeeMapper;
import com.mtesazi.employeeservice.repository.EmployeeRepository;
import com.mtesazi.employeeservice.service.DepartmentLookupService;
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
    private final DepartmentLookupService departmentLookupService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        departmentLookupService.validateDepartmentExists(request.getDepartment());
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
        departmentLookupService.validateDepartmentExists(request.getDepartment());
        Employee employee = findEmployeeOrThrow(id);
        employeeMapper.applyRequestToEntity(request, employee);
        Employee updatedEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(updatedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailsResponse getEmployeeDetails(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        DepartmentResponse department = resolveDepartment(employee.getDepartment());
        return new EmployeeDetailsResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                resolveDepartmentId(employee.getDepartment(), department),
                department
        );
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

    private DepartmentResponse resolveDepartment(String departmentReference) {
        if (departmentReference == null || departmentReference.isBlank()) {
            return null;
        }
        return departmentLookupService.findDepartmentByReference(departmentReference);
    }

    /**
     * Prefers the id reported by the department service, falling back to the employee's own
     * numeric reference. That keeps {@code departmentId} populated when the department
     * service is degraded and the lookup returns the circuit-breaker fallback payload.
     */
    private Long resolveDepartmentId(String departmentReference, DepartmentResponse department) {
        if (department != null && department.id() != null) {
            return department.id();
        }
        if (departmentReference != null && departmentReference.matches("\\d+")) {
            return Long.parseLong(departmentReference);
        }
        return null;
    }
}
