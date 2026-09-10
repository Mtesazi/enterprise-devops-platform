package com.mtesazi.employeeservice;

import com.mtesazi.employeeservice.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.main.lazy-initialization=true",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "services.department.base-url=http://department-service",
                "services.department.connect-timeout=100ms",
                "services.department.read-timeout=100ms",
                "management.endpoints.web.exposure.include=health,info,prometheus,metrics",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        }
)
@AutoConfigureObservability
class EmployeeActuatorIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void healthEndpointReportsUp() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void prometheusEndpointIsExposed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));

        ResponseEntity<String> response = testRestTemplate.exchange(
                "http://localhost:" + port + "/actuator/prometheus",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("jvm_"));
    }
}
