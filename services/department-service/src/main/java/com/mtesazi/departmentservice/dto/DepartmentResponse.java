package com.mtesazi.departmentservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
