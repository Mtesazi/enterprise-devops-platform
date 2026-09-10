package com.mtesazi.departmentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DeleteDepartmentResponse", description = "Delete department success response")
public class DeleteDepartmentResponse {

    @Schema(description = "Deleted department ID", example = "1")
    private Long id;

    @Schema(description = "Delete status message", example = "Department 1 deleted successfully")
    private String message;

    public DeleteDepartmentResponse() {
    }

    public DeleteDepartmentResponse(Long id, String message) {
        this.id = id;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
