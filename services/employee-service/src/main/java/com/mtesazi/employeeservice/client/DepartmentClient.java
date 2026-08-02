package com.mtesazi.employeeservice.client;

import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.config.DepartmentServiceClientProperties;
import com.mtesazi.employeeservice.exception.DepartmentReferenceNotFoundException;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import com.mtesazi.employeeservice.exception.DepartmentServiceTimeoutException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

@Component
public class DepartmentClient {

    private static final ParameterizedTypeReference<List<DepartmentResponse>> DEPARTMENT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final DepartmentServiceClientProperties properties;

    public DepartmentClient(RestClient.Builder restClientBuilder, DepartmentServiceClientProperties properties) {
        this.restClient = restClientBuilder
                .requestFactory(createRequestFactory(properties))
                .baseUrl(properties.getBaseUrl())
                .build();
        this.properties = properties;
    }

    public void validateDepartmentExists(String departmentReference) {
        if (departmentReference == null || departmentReference.isBlank()) {
            return;
        }
        findDepartmentByReference(departmentReference);
    }

    public DepartmentResponse findDepartmentByReference(String departmentReference) {
        if (departmentReference == null || departmentReference.isBlank()) {
            return null;
        }

        return fetchDepartments().stream()
                .filter(department -> departmentReference.equalsIgnoreCase(department.code())
                        || departmentReference.equalsIgnoreCase(department.name()))
                .findFirst()
                .orElseThrow(() -> new DepartmentReferenceNotFoundException(
                        "Department '" + departmentReference + "' does not exist"));
    }

    private List<DepartmentResponse> fetchDepartments() {
        try {
            List<DepartmentResponse> departments = restClient.get()
                    .uri("/api/v1/departments")
                    .retrieve()
                    .body(DEPARTMENT_LIST_TYPE);
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

    private SimpleClientHttpRequestFactory createRequestFactory(DepartmentServiceClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        return requestFactory;
    }
}
