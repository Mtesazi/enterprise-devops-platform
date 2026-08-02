package com.mtesazi.employeeservice.client;

import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.config.DepartmentServiceClientProperties;
import com.mtesazi.employeeservice.config.RestClientConfig;
import com.mtesazi.employeeservice.service.DepartmentLookupService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = DepartmentClientLoadBalancingIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=true",
                "eureka.client.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "services.department.base-url=http://DEPARTMENT-SERVICE",
                "services.department.connect-timeout=100ms",
                "services.department.read-timeout=100ms",
                "spring.cloud.discovery.client.simple.instances.DEPARTMENT-SERVICE[0].uri=http://localhost:${test.stopped-port}",
                "spring.cloud.discovery.client.simple.instances.DEPARTMENT-SERVICE[1].uri=http://localhost:${test.healthy-port}",
                "resilience4j.retry.instances.departmentService.max-attempts=2",
                "resilience4j.retry.instances.departmentService.wait-duration=10ms",
                "resilience4j.circuitbreaker.instances.departmentService.sliding-window-size=2",
                "resilience4j.circuitbreaker.instances.departmentService.minimum-number-of-calls=2",
                "resilience4j.circuitbreaker.instances.departmentService.failure-rate-threshold=50",
                "resilience4j.circuitbreaker.instances.departmentService.wait-duration-in-open-state=1s"
        }
)
class DepartmentClientLoadBalancingIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({RestClientConfig.class, DepartmentServiceClientProperties.class, DepartmentClient.class, DepartmentLookupService.class})
    static class TestConfig {
    }

    private static final AtomicInteger HEALTHY_REQUEST_COUNT = new AtomicInteger();
    private static int stoppedPort;
    private static int healthyPort;

    static {
        stoppedPort = reserveFreePort();
        healthyPort = reserveFreePort();
        System.setProperty("test.stopped-port", Integer.toString(stoppedPort));
        System.setProperty("test.healthy-port", Integer.toString(healthyPort));
    }

    @jakarta.annotation.Resource
    private DepartmentLookupService departmentLookupService;

    private HttpServer healthyServer;

    @BeforeEach
    void startHealthyServer() throws IOException {
        HEALTHY_REQUEST_COUNT.set(0);
        healthyServer = HttpServer.create(new InetSocketAddress(healthyPort), 0);
        healthyServer.createContext("/api/v1/departments", exchange -> {
            HEALTHY_REQUEST_COUNT.incrementAndGet();
            byte[] body = "[{\"id\":10,\"name\":\"Engineering\",\"code\":\"ENG\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        healthyServer.start();
    }

    @AfterEach
    void stopHealthyServer() {
        if (healthyServer != null) {
            healthyServer.stop(0);
        }
    }

    @Test
    void retriesAgainstRemainingInstanceWhenOneInstanceIsUnavailable() {
        DepartmentResponse response = departmentLookupService.findDepartmentByReference("ENG");

        assertEquals("Engineering", response.name());
        assertEquals("ENG", response.code());
        assertEquals(1, HEALTHY_REQUEST_COUNT.get());
    }

    private static int reserveFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reserve free port", exception);
        }
    }
}
