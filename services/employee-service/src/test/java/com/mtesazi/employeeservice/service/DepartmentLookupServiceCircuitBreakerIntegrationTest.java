package com.mtesazi.employeeservice.service;

import com.mtesazi.employeeservice.client.DepartmentClient;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = DepartmentLookupServiceCircuitBreakerIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "resilience4j.retry.instances.departmentService.max-attempts=1",
                "resilience4j.circuitbreaker.instances.departmentService.sliding-window-size=2",
                "resilience4j.circuitbreaker.instances.departmentService.minimum-number-of-calls=2",
                "resilience4j.circuitbreaker.instances.departmentService.failure-rate-threshold=50",
                "resilience4j.circuitbreaker.instances.departmentService.wait-duration-in-open-state=1s"
        }
)
class DepartmentLookupServiceCircuitBreakerIntegrationTest {

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
    void opensCircuitBreakerAfterRepeatedCommunicationFailures() {
        when(departmentClient.findDepartmentByReference("ENG"))
                .thenThrow(new DepartmentServiceCommunicationException("Could not reach department service"));

        assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.findDepartmentByReference("ENG")
        );

        DepartmentServiceCommunicationException exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.findDepartmentByReference("ENG")
        );

        assertEquals("Could not reach department service", exception.getMessage());

        exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.findDepartmentByReference("ENG")
        );

        assertEquals("Department service circuit breaker is open", exception.getMessage());
    }
}
