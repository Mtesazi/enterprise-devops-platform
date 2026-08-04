package com.mtesazi.departmentservice.controller;

import com.mtesazi.departmentservice.dto.DepartmentResponse;
import com.mtesazi.departmentservice.service.DepartmentService;
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

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentService departmentService;

    @Test
    void getDepartmentByIdIsAvailableAtNewApiPath() throws Exception {
        DepartmentResponse response = new DepartmentResponse();
        response.setId(7L);
        response.setName("Information Technology");
        response.setCode("IT");

        when(departmentService.getDepartmentById(7L)).thenReturn(response);

        mockMvc.perform(get("/api/departments/7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Information Technology"))
                .andExpect(jsonPath("$.code").value("IT"));

        verify(departmentService).getDepartmentById(7L);
    }
}
