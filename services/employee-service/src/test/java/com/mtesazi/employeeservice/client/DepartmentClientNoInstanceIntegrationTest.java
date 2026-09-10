package com.mtesazi.employeeservice.client;

import com.mtesazi.employeeservice.config.DepartmentServiceClientProperties;
import com.mtesazi.employeeservice.config.RestClientConfig;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import com.mtesazi.employeeservice.service.DepartmentLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The department service is not registered with discovery at all, so Spring Cloud
 * LoadBalancer cannot pick an instance. That must surface as a department-service
 * communication failure (HTTP 503) rather than leaking an {@code IllegalStateException}
 * out as a generic HTTP 500.
 */
@SpringBootTest(
        classes = DepartmentClientNoInstanceIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=true",
                "eureka.client.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "services.department.base-url=http://department-service",
                "services.department.connect-timeout=100ms",
                "services.department.read-timeout=200ms",
                // Only an unrelated service is registered: department-service has no instance.
                "spring.cloud.discovery.client.simple.instances.payroll-service[0].uri=http://localhost:1",
                "resilience4j.retry.instances.departmentService.max-attempts=2",
                "resilience4j.retry.instances.departmentService.wait-duration=10ms"
        }
)
class DepartmentClientNoInstanceIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({RestClientConfig.class, DepartmentServiceClientProperties.class, DepartmentClient.class,
            DepartmentLookupService.class})
    static class TestConfig {
    }

    @jakarta.annotation.Resource
    private DepartmentClient departmentClient;

    @jakarta.annotation.Resource
    private DepartmentLookupService departmentLookupService;

    @Test
    void mapsAnEmptyLoadBalancerInstanceListToACommunicationFailure() {
        DepartmentServiceCommunicationException exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentClient.findDepartmentByReference("1")
        );

        assertEquals("No department service instance is available from service discovery", exception.getMessage());
    }

    @Test
    void rejectsEmployeeCreationWhenDepartmentServiceIsNotDiscoverable() {
        assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> departmentLookupService.validateDepartmentExists("1")
        );
    }
}
