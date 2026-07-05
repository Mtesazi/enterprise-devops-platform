package com.mtesazi.departmentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "DeleteDepartmentResponse", description = "Delete department success response")
public class DeleteDepartmentResponse {

    @Schema(description = "Deleted department ID", example = "1")
    private Long id;

    @Schema(description = "Delete status message", example = "Department 1 deleted successfully")
    private String message;
}
