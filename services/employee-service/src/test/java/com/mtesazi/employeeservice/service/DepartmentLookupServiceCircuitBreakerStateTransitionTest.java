package com.mtesazi.employeeservice.service;

import com.mtesazi.employeeservice.client.DepartmentClient;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * Walks the department-service circuit breaker through its full lifecycle:
 * CLOSED -> OPEN -> HALF_OPEN -> CLOSED.
 */
@SpringBootTest(
        classes = DepartmentLookupServiceCircuitBreakerStateTransitionTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "resilience4j.retry.instances.departmentService.max-attempts=1",
                "resilience4j.circuitbreaker.instances.departmentService.sliding-window-type=COUNT_BASED",
                "resilience4j.circuitbreaker.instances.departmentService.sliding-window-size=4",
                "resilience4j.circuitbreaker.instances.departmentService.minimum-number-of-calls=4",
                "resilience4j.circuitbreaker.instances.departmentService.failure-rate-threshold=50",
                "resilience4j.circuitbreaker.instances.departmentService.wait-duration-in-open-state=300ms",
                "resilience4j.circuitbreaker.instances.departmentService.permitted-number-of-calls-in-half-open-state=2",
                "resilience4j.circuitbreaker.instances.departmentService.automatic-transition-from-open-to-half-open-enabled=true"
        }
)
class DepartmentLookupServiceCircuitBreakerStateTransitionTest {

    private static final String DEPARTMENT_REFERENCE = "1";
    private static final long STATE_CHANGE_TIMEOUT_MILLIS = 5_000;

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

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("departmentService");
        circuitBreaker.reset();
    }

    @Test
    void transitionsFromClosedToOpenToHalfOpenAndBackToClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        doThrow(new DepartmentServiceCommunicationException("Could not reach department service"))
                .when(departmentClient)
                .validateDepartmentExists(DEPARTMENT_REFERENCE);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThrows(
                    DepartmentServiceCommunicationException.class,
                    () -> departmentLookupService.validateDepartmentExists(DEPARTMENT_REFERENCE)
            );
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // While OPEN the department service is not called at all: the fallback answers.
        DepartmentServiceCommunicationException shortCircuited = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.validateDepartmentExists(DEPARTMENT_REFERENCE)
        );
        assertEquals("Department service circuit breaker is open", shortCircuited.getMessage());

        Mockito.reset(departmentClient);
        doNothing().when(departmentClient).validateDepartmentExists(DEPARTMENT_REFERENCE);

        awaitState(CircuitBreaker.State.HALF_OPEN);

        // Two probe calls succeed, which is the configured half-open permit count.
        departmentLookupService.validateDepartmentExists(DEPARTMENT_REFERENCE);
        departmentLookupService.validateDepartmentExists(DEPARTMENT_REFERENCE);

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void reopensWhenHalfOpenProbeCallsStillFail() {
        doThrow(new DepartmentServiceCommunicationException("Could not reach department service"))
                .when(departmentClient)
                .validateDepartmentExists(DEPARTMENT_REFERENCE);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThrows(
                    DepartmentServiceCommunicationException.class,
                    () -> departmentLookupService.validateDepartmentExists(DEPARTMENT_REFERENCE)
            );
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        awaitState(CircuitBreaker.State.HALF_OPEN);

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThrows(
                    DepartmentServiceCommunicationException.class,
                    () -> departmentLookupService.validateDepartmentExists(DEPARTMENT_REFERENCE)
            );
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    private void awaitState(CircuitBreaker.State expectedState) {
        long deadline = System.currentTimeMillis() + STATE_CHANGE_TIMEOUT_MILLIS;
        while (circuitBreaker.getState() != expectedState && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertEquals(expectedState, circuitBreaker.getState());
    }
}
