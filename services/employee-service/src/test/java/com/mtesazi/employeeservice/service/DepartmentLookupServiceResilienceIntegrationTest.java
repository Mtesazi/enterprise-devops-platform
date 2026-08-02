package com.mtesazi.employeeservice.service;

import com.mtesazi.employeeservice.client.DepartmentClient;
import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.exception.DepartmentServiceTimeoutException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = DepartmentLookupServiceResilienceIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "resilience4j.retry.instances.departmentService.max-attempts=3",
                "resilience4j.retry.instances.departmentService.wait-duration=1ms",
                "resilience4j.circuitbreaker.instances.departmentService.sliding-window-size=10",
                "resilience4j.circuitbreaker.instances.departmentService.minimum-number-of-calls=10",
                "resilience4j.circuitbreaker.instances.departmentService.failure-rate-threshold=50",
                "resilience4j.circuitbreaker.instances.departmentService.wait-duration-in-open-state=1s"
        }
)
class DepartmentLookupServiceResilienceIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(DepartmentLookupService.class)
    static class TestConfig {
    }

    @MockBean
    private DepartmentClient departmentClient;

    @jakarta.annotation.Resource
    private DepartmentLookupService departmentLookupService;

    @jakarta.annotation.Resource
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("departmentService").reset();
    }

    @Test
    void retriesTransientTimeoutsBeforeReturningDepartment() {
        DepartmentResponse response = new DepartmentResponse(10L, "Engineering", "ENG");

        when(departmentClient.findDepartmentByReference("ENG"))
                .thenThrow(new DepartmentServiceTimeoutException("timeout-1", new RuntimeException("timeout-1")))
                .thenThrow(new DepartmentServiceTimeoutException("timeout-2", new RuntimeException("timeout-2")))
                .thenReturn(response);

        DepartmentResponse result = departmentLookupService.findDepartmentByReference("ENG");

        assertEquals(response, result);
        verify(departmentClient, times(3)).findDepartmentByReference("ENG");
    }

    @Test
    void returnsUnavailableDepartmentWhenRetriesAreExhausted() {
        when(departmentClient.findDepartmentByReference("ENG"))
                .thenThrow(new DepartmentServiceTimeoutException("timeout-1", new RuntimeException("timeout-1")))
                .thenThrow(new DepartmentServiceTimeoutException("timeout-2", new RuntimeException("timeout-2")))
                .thenThrow(new DepartmentServiceTimeoutException("timeout-3", new RuntimeException("timeout-3")));

        DepartmentResponse result = departmentLookupService.findDepartmentByReference("ENG");

        assertEquals("Department Service Unavailable", result.name());
        assertEquals("N/A", result.code());
        verify(departmentClient, times(3)).findDepartmentByReference("ENG");
    }
}
