package com.mtesazi.departmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    private String name;

    @NotBlank(message = "Department code is required")
    private String code;

    private String description;

}
