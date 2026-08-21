package com.mtesazi.employeeservice.client;

import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.config.DepartmentServiceClientProperties;
import com.mtesazi.employeeservice.exception.DepartmentReferenceNotFoundException;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import com.mtesazi.employeeservice.exception.DepartmentServiceTimeoutException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link DepartmentClient} against a real HTTP server so that transport-level
 * failure modes (timeouts, refused connections) are reproduced rather than mocked.
 */
class DepartmentClientTest {

    private static final String DEPARTMENT_JSON =
            "{\"id\":1,\"name\":\"Engineering\",\"code\":\"ENG\",\"description\":\"Engineering department\"}";
    private static final String DEPARTMENT_LIST_JSON = "[" + DEPARTMENT_JSON + "]";

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsDepartmentWhenReferenceIsANumericId() {
        stub("/api/departments/1", json(200, DEPARTMENT_JSON));

        DepartmentResponse department = clientForServer().findDepartmentByReference("1");

        assertEquals(1L, department.id());
        assertEquals("Engineering", department.name());
        assertEquals("ENG", department.code());
        assertEquals("Engineering department", department.description());
    }

    @Test
    void returnsDepartmentWhenReferenceIsACode() {
        stub("/api/departments", json(200, DEPARTMENT_LIST_JSON));

        DepartmentResponse department = clientForServer().findDepartmentByReference("eng");

        assertEquals(1L, department.id());
        assertEquals("ENG", department.code());
    }

    @Test
    void returnsNullForBlankReferenceWithoutCallingDepartmentService() {
        AtomicInteger requestCount = new AtomicInteger();
        stub("/api/departments", exchange -> {
            requestCount.incrementAndGet();
            json(200, DEPARTMENT_LIST_JSON).handle(exchange);
        });

        DepartmentClient client = clientForServer();

        assertNull(client.findDepartmentByReference(null));
        assertNull(client.findDepartmentByReference("   "));
        assertEquals(0, requestCount.get());
    }

    @Test
    void validateDepartmentExistsAcceptsBlankReference() {
        // No stub registered: a request would fail with a 404, so this only passes if the
        // blank reference short-circuits before any call is made.
        clientForServer().validateDepartmentExists("");
    }

    @Test
    void throwsDepartmentReferenceNotFoundWhenDepartmentIdIsUnknown() {
        stub("/api/departments/99", status(404));

        DepartmentClient client = clientForServer();

        DepartmentReferenceNotFoundException exception = assertThrows(
                DepartmentReferenceNotFoundException.class,
                () -> client.findDepartmentByReference("99")
        );

        assertEquals("Department '99' does not exist", exception.getMessage());
    }

    @Test
    void throwsDepartmentReferenceNotFoundWhenCodeIsNotInTheDepartmentList() {
        stub("/api/departments", json(200, DEPARTMENT_LIST_JSON));

        DepartmentClient client = clientForServer();

        DepartmentReferenceNotFoundException exception = assertThrows(
                DepartmentReferenceNotFoundException.class,
                () -> client.findDepartmentByReference("SALES")
        );

        assertEquals("Department 'SALES' does not exist", exception.getMessage());
    }

    @Test
    void translatesUnexpectedClientErrorsIntoCommunicationFailures() {
        stub("/api/departments/1", status(400));

        DepartmentClient client = clientForServer();

        DepartmentServiceCommunicationException exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> client.findDepartmentByReference("1")
        );

        assertTrue(exception.getMessage().contains("400"), exception.getMessage());
    }

    @Test
    void translatesServerErrorsIntoCommunicationFailures() {
        stub("/api/departments/1", status(500));

        DepartmentClient client = clientForServer();

        DepartmentServiceCommunicationException exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> client.findDepartmentByReference("1")
        );

        assertTrue(exception.getMessage().contains("500"), exception.getMessage());
    }

    @Test
    void translatesServerErrorsOnTheDepartmentListIntoCommunicationFailures() {
        stub("/api/departments", status(503));

        DepartmentClient client = clientForServer();

        assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> client.findDepartmentByReference("ENG")
        );
    }

    @Test
    void translatesMissingDepartmentListEndpointIntoCommunicationFailure() {
        // A 404 on the collection endpoint means the endpoint is gone, not that a
        // particular department is unknown.
        stub("/api/departments", status(404));

        DepartmentClient client = clientForServer();

        assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> client.findDepartmentByReference("ENG")
        );
    }

    @Test
    void translatesSlowResponsesIntoTimeouts() {
        stub("/api/departments/1", exchange -> {
            sleep(1_000);
            json(200, DEPARTMENT_JSON).handle(exchange);
        });

        DepartmentClient client = client(properties("http://localhost:" + port,
                Duration.ofMillis(500), Duration.ofMillis(200)));

        DepartmentServiceTimeoutException exception = assertThrows(
                DepartmentServiceTimeoutException.class,
                () -> client.findDepartmentByReference("1")
        );

        assertEquals("Department service timed out after 200ms", exception.getMessage());
    }

    @Test
    void translatesAnUnreachableDepartmentServiceIntoCommunicationFailure() {
        DepartmentClient client = client(properties("http://localhost:" + unusedPort(),
                Duration.ofMillis(500), Duration.ofMillis(500)));

        DepartmentServiceCommunicationException exception = assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> client.findDepartmentByReference("1")
        );

        assertEquals("Could not reach department service", exception.getMessage());
    }

    @Test
    void translatesAnEmptyResponseBodyIntoCommunicationFailure() {
        stub("/api/departments/1", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        DepartmentClient client = clientForServer();

        assertThrows(
                DepartmentServiceCommunicationException.class,
                () -> client.findDepartmentByReference("1")
        );
    }

    @Test
    void rejectsAMissingBaseUrl() {
        DepartmentServiceClientProperties properties =
                properties(" ", Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThrows(IllegalArgumentException.class, () -> client(properties));
    }

    private DepartmentClient clientForServer() {
        return client(properties("http://localhost:" + port, Duration.ofSeconds(2), Duration.ofSeconds(2)));
    }

    private DepartmentClient client(DepartmentServiceClientProperties properties) {
        return new DepartmentClient(RestClient.builder(), properties);
    }

    private DepartmentServiceClientProperties properties(String baseUrl,
                                                         Duration connectTimeout,
                                                         Duration readTimeout) {
        DepartmentServiceClientProperties properties = new DepartmentServiceClientProperties();
        properties.setBaseUrl(baseUrl);
        properties.setConnectTimeout(connectTimeout);
        properties.setReadTimeout(readTimeout);
        return properties;
    }

    private void stub(String path, HttpHandler handler) {
        server.createContext(path, handler);
    }

    private HttpHandler json(int status, String body) {
        return exchange -> {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(payload);
            }
        };
    }

    private HttpHandler status(int status) {
        return (HttpExchange exchange) -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        };
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private int unusedPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reserve free port", exception);
        }
    }
}
