package com.mtesazi.employeeservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate departmentServiceRestTemplate(
            RestTemplateBuilder restTemplateBuilder,
            DepartmentServiceClientProperties properties) {
        return restTemplateBuilder
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .build();
    }

}
