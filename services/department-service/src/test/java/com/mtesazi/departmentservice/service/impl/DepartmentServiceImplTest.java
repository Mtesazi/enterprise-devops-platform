package com.mtesazi.departmentservice.service.impl;

import com.mtesazi.departmentservice.dto.DepartmentResponse;
import com.mtesazi.departmentservice.entity.Department;
import com.mtesazi.departmentservice.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(DepartmentServiceImpl.class)
class DepartmentServiceImplTest {

    @Autowired
    private DepartmentServiceImpl departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void getsDepartmentByReferenceFromCode() {
        Department department = new Department();
        department.setName("Engineering");
        department.setCode("ENG");
        department.setDescription("Product and platform engineering");
        departmentRepository.saveAndFlush(department);

        DepartmentResponse response = departmentService.getDepartmentByReference("eng");

        assertEquals("Engineering", response.getName());
        assertEquals("ENG", response.getCode());
    }
}
