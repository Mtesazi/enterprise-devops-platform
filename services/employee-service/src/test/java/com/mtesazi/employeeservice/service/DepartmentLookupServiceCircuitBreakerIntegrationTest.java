package com.mtesazi.employeeservice.service;

import com.mtesazi.employeeservice.client.DepartmentClient;
import com.mtesazi.employeeservice.exception.DepartmentReferenceNotFoundException;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        doThrow(new DepartmentServiceCommunicationException("Could not reach department service"))
                .when(departmentClient)
                .validateDepartmentExists("ENG");

        assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.validateDepartmentExists("ENG")
        );

        DepartmentServiceCommunicationException exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.validateDepartmentExists("ENG")
        );

        assertEquals("Could not reach department service", exception.getMessage());

        exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.validateDepartmentExists("ENG")
        );

        assertEquals("Department service circuit breaker is open", exception.getMessage());
    }

    @Test
    void unknownDepartmentReferencesDoNotOpenTheCircuitBreaker() {
        // Rejecting bad input is not a department-service outage, so these calls must be
        // ignored by the breaker (see resilience4j.circuitbreaker...ignore-exceptions).
        doThrow(new DepartmentReferenceNotFoundException("Department 'NOPE' does not exist"))
                .when(departmentClient)
                .validateDepartmentExists("NOPE");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(
                    DepartmentReferenceNotFoundException.class,
                    () -> departmentLookupService.validateDepartmentExists("NOPE")
            );
        }

        assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreakerRegistry.circuitBreaker("departmentService").getState()
        );
    }
}
