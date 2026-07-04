package com.mtesazi.employee.controller;

import com.mtesazi.employee.dto.DeleteEmployeeResponse;
import com.mtesazi.employee.dto.EmployeeRequest;
import com.mtesazi.employee.dto.EmployeeResponse;
import com.mtesazi.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @Parameter(description = "Employee ID", example = "1")
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @Parameter(description = "Employee ID", example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee by ID")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employee deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteEmployeeResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content)
    })
    public ResponseEntity<DeleteEmployeeResponse> deleteEmployee(
            @Parameter(description = "Employee ID", example = "1")
            @PathVariable("id") Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(new DeleteEmployeeResponse(id, "Employee with id:"   + id + " is deleted successfully"));
    }
}