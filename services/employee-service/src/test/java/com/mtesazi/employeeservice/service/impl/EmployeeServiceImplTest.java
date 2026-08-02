package com.mtesazi.employeeservice.service.impl;

import com.mtesazi.employeeservice.client.DepartmentClient;
import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.dto.EmployeeDetailsResponse;
import com.mtesazi.employeeservice.entity.Employee;
import com.mtesazi.employeeservice.exception.EmployeeNotFoundException;
import com.mtesazi.employeeservice.mapper.EmployeeMapper;
import com.mtesazi.employeeservice.repository.EmployeeRepository;
import com.mtesazi.employeeservice.service.DepartmentLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DepartmentLookupService departmentLookupService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void getEmployeeDetailsByIdCombinesEmployeeAndDepartmentResponses() {
        Employee employee = new Employee(
                1L,
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "ENG",
                BigDecimal.valueOf(125000),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        DepartmentResponse department = new DepartmentResponse(10L, "Engineering", "ENG");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(departmentLookupService.findDepartmentByReference("ENG")).thenReturn(department);

        EmployeeDetailsResponse response = employeeService.getEmployeeDetailsById(1L);

        assertEquals(1L, response.id());
        assertEquals("Jane", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("jane.doe@example.com", response.email());
        assertEquals(10L, response.departmentId());
        assertEquals(department, response.department());
        verify(departmentLookupService).findDepartmentByReference("ENG");
    }

    @Test
    void getEmployeeDetailsByIdReturnsControlledDepartmentFallback() {
        Employee employee = new Employee(
                1L,
                "Bongani",
                "Gumede",
                "bongani.gumede@example.com",
                "ENG",
                BigDecimal.valueOf(125000),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        DepartmentResponse fallbackDepartment = new DepartmentResponse(null, "Department Service Unavailable", "N/A");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(departmentLookupService.findDepartmentByReference("ENG")).thenReturn(fallbackDepartment);

        EmployeeDetailsResponse response = employeeService.getEmployeeDetailsById(1L);

        assertEquals(1L, response.id());
        assertEquals("Bongani", response.firstName());
        assertEquals("Gumede", response.lastName());
        assertEquals("bongani.gumede@example.com", response.email());
        assertEquals(null, response.departmentId());
        assertEquals(fallbackDepartment, response.department());
    }

    @Test
    void getEmployeeDetailsByIdThrowsWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        EmployeeNotFoundException exception = assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.getEmployeeDetailsById(99L)
        );

        assertEquals("Employee 99 not found", exception.getMessage());
        verify(departmentLookupService, never()).findDepartmentByReference(org.mockito.ArgumentMatchers.anyString());
    }
}
