package com.mtesazi.gatewayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "services")
public class GatewayServicesProperties {

    private final ServiceTarget employee = new ServiceTarget();
    private final ServiceTarget department = new ServiceTarget();
    private final ServiceTarget auth = new ServiceTarget();

    public ServiceTarget getEmployee() {
        return employee;
    }

    public ServiceTarget getDepartment() {
        return department;
    }

    public ServiceTarget getAuth() {
        return auth;
    }

    public static class ServiceTarget {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
