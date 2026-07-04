package com.mtesazi.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "DeleteEmployeeResponse", description = "Delete employee success response")
public class DeleteEmployeeResponse {

    @Schema(description = "Deleted employee ID", example = "1")
    private Long id;

    @Schema(description = "Delete status message", example = "Employee 1 deleted successfully")
    private String message;
}
