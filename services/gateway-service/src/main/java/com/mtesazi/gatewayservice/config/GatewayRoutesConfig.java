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
                .route("auth-service-login-route", r -> r
                        .path("/login", "/register", "/refresh", "/me")
                        .uri(properties.getAuth().getBaseUrl()))
                .route("auth-service-route", r -> r
                        .path("/api/auth/**")
                        .uri(properties.getAuth().getBaseUrl()))
                .route("employee-service-route", r -> r
                        .path("/api/v1/employees/**")
                        .uri(properties.getEmployee().getBaseUrl()))
                .route("employee-service-compat-route", r -> r
                        .path("/api/employees/**")
                        .filters(f -> f.rewritePath("/api/employees/(?<segment>.*)", "/api/v1/employees/${segment}"))
                        .uri(properties.getEmployee().getBaseUrl()))
                .route("department-service-route", r -> r
                        .path("/api/v1/departments/**")
                        .uri(properties.getDepartment().getBaseUrl()))
                .route("department-service-compat-route", r -> r
                        .path("/api/departments/**")
                        .filters(f -> f.rewritePath("/api/departments/(?<segment>.*)", "/api/v1/departments/${segment}"))
                        .uri(properties.getDepartment().getBaseUrl()))
                .build();
    }
}
