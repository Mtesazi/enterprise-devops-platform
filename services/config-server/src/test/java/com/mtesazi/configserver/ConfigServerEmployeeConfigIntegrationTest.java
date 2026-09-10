package com.mtesazi.configserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerEmployeeConfigIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void servesCentralizedEmployeeServiceConfiguration() throws Exception {
        ResponseEntity<String> response =
                testRestTemplate.getForEntity("http://localhost:" + port + "/employee-service/default", String.class);

        assertEquals(200, response.getStatusCode().value());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode propertySources = root.path("propertySources");
        assertTrue(propertySources.isArray() && !propertySources.isEmpty());

        String serialized = propertySources.toString();
        assertTrue(serialized.contains("\"services.department.base-url\":\"http://DEPARTMENT-SERVICE\""));
        assertTrue(serialized.contains("\"management.endpoints.web.exposure.include\":\"health,info,prometheus,metrics\""));
    }
}
