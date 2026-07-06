package com.mtesazi.employeeservice.integration;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DepartmentSummary {

    private Long id;
    private String name;
    private String code;
    private String description;

}
