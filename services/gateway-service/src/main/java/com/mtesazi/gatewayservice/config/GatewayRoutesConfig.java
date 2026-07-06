package com.mtesazi.gatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator serviceRoutes(RouteLocatorBuilder builder, GatewayServicesProperties properties) {
        return builder.routes()
                .route("employee-service-route", r -> r
                        .path("/api/v1/employees/**")
                        .uri(properties.getEmployee().getBaseUrl()))
                .route("department-service-route", r -> r
                        .path("/api/v1/departments/**")
                        .uri(properties.getDepartment().getBaseUrl()))
                .build();
    }
}
