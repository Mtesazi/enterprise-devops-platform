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

    /**
     * Logical Eureka service id of the department service. Kept as a service id (not a
     * host:port) so that the load-balanced RestClient resolves it through discovery.
     */
    private String baseUrl = "http://department-service";

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration readTimeout = Duration.ofSeconds(3);

}
