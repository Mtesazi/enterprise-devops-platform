package com.mtesazi.employeeservice.controller;

import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.dto.EmployeeDetailsResponse;
import com.mtesazi.employeeservice.exception.EmployeeNotFoundException;
import com.mtesazi.employeeservice.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void getEmployeeDetailsByIdReturnsAggregatedEmployeePayload() throws Exception {
        EmployeeDetailsResponse response = new EmployeeDetailsResponse(
                1L,
                "Jane",
                "Doe",
                "jane.doe@example.com",
                10L,
                new DepartmentResponse(10L, "Engineering", "ENG", "Engineering department")
        );

        when(employeeService.getEmployeeDetailsById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/employees/{id}/details", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.departmentId").value(10))
                .andExpect(jsonPath("$.department.id").value(10))
                .andExpect(jsonPath("$.department.name").value("Engineering"))
                .andExpect(jsonPath("$.department.code").value("ENG"));

        verify(employeeService).getEmployeeDetailsById(1L);
    }

    @Test
    void getEmployeeDetailsByIdReturnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        when(employeeService.getEmployeeDetailsById(999L))
                .thenThrow(new EmployeeNotFoundException("Employee 999 not found"));

        mockMvc.perform(get("/api/v1/employees/{id}/details", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee 999 not found"));
    }
}
