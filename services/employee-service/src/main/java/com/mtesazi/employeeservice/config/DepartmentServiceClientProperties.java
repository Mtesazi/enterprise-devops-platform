package com.mtesazi.employeeservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "services.department")
public class DepartmentServiceClientProperties {

    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;

}
