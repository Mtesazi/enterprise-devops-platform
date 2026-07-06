package com.mtesazi.employeeservice.integration;

import com.mtesazi.employeeservice.config.DepartmentServiceClientProperties;
import com.mtesazi.employeeservice.exception.DepartmentReferenceNotFoundException;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import com.mtesazi.employeeservice.exception.DepartmentServiceTimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DepartmentServiceClient {

    private static final ParameterizedTypeReference<List<DepartmentSummary>> DEPARTMENT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate departmentServiceRestTemplate;
    private final DepartmentServiceClientProperties properties;

    public void validateDepartmentExists(String departmentReference) {
        if (departmentReference == null || departmentReference.isBlank()) {
            return;
        }

        List<DepartmentSummary> departments = fetchDepartments();
        boolean exists = departments.stream()
                .anyMatch(department -> departmentReference.equalsIgnoreCase(department.getCode())
                        || departmentReference.equalsIgnoreCase(department.getName()));

        if (!exists) {
            throw new DepartmentReferenceNotFoundException(
                    "Department '" + departmentReference + "' does not exist");
        }
    }

    private List<DepartmentSummary> fetchDepartments() {
        String uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/api/v1/departments")
                .toUriString();

        try {
            ResponseEntity<List<DepartmentSummary>> response = departmentServiceRestTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    DEPARTMENT_LIST_TYPE
            );

            List<DepartmentSummary> departments = response.getBody();
            if (departments == null) {
                throw new DepartmentServiceCommunicationException("Department service returned an empty response body");
            }
            return departments;
        } catch (ResourceAccessException ex) {
            if (containsCause(ex, SocketTimeoutException.class)) {
                Duration timeout = properties.getReadTimeout();
                throw new DepartmentServiceTimeoutException(
                        "Department service timed out after " + timeout.toMillis() + "ms",
                        ex
                );
            }
            throw new DepartmentServiceCommunicationException("Could not reach department service", ex);
        } catch (HttpStatusCodeException ex) {
            throw new DepartmentServiceCommunicationException(
                    "Department service request failed with status " + ex.getStatusCode(),
                    ex
            );
        }
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
