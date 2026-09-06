package com.mtesazi.gatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator serviceRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service-login-route", r -> r
                        .path("/login", "/register", "/refresh", "/me")
                        .uri("lb://auth-service"))
                .route("auth-service-route", r -> r
                        .path("/api/auth/**")
                        .uri("lb://auth-service"))
                .route("employee-service-route", r -> r
                        .path("/api/v1/employees/**", "/api/employees/**")
                        .uri("lb://employee-service"))
                .route("department-service-route", r -> r
                        .path("/api/v1/departments/**", "/api/departments/**")
                        .uri("lb://department-service"))
                .build();
    }
}
