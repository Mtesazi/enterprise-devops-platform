package com.mtesazi.employeeservice.client;

import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.config.DepartmentServiceClientProperties;
import com.mtesazi.employeeservice.exception.DepartmentReferenceNotFoundException;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import com.mtesazi.employeeservice.exception.DepartmentServiceTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Talks to the Department Service through its logical Eureka service id
 * (see {@code services.department.base-url}), resolved at call time by Spring Cloud
 * LoadBalancer. Every transport and protocol failure is translated into one of the
 * service's own exceptions so that Resilience4j retry/circuit-breaker policies - which
 * are configured per exception type - can classify failures correctly.
 */
@Component
public class DepartmentClient {

    private static final Logger log = LoggerFactory.getLogger(DepartmentClient.class);

    private static final String DEPARTMENTS_PATH = "/api/departments";
    private static final String DEPARTMENT_BY_ID_PATH = DEPARTMENTS_PATH + "/{id}";

    /**
     * Messages used by {@code BlockingLoadBalancerClient} when discovery yields no instance
     * for the logical service id. Which one is raised depends on the interceptor in play:
     * {@code LoadBalancerInterceptor} reports the first, {@code RetryLoadBalancerInterceptor}
     * (installed when spring-retry is on the classpath) reports the second.
     */
    private static final List<String> NO_INSTANCE_MESSAGES =
            List.of("No instances available", "Service Instance cannot be null");

    private final RestClient restClient;
    private final DepartmentServiceClientProperties properties;

    public DepartmentClient(RestClient.Builder restClientBuilder,
                            DepartmentServiceClientProperties properties) {
        Assert.hasText(properties.getBaseUrl(), "services.department.base-url must be configured");
        Assert.notNull(properties.getConnectTimeout(), "services.department.connect-timeout must be configured");
        Assert.notNull(properties.getReadTimeout(), "services.department.read-timeout must be configured");
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(createRequestFactory(properties))
                .build();
        this.properties = properties;
    }

    /**
     * Fails fast when {@code departmentReference} does not identify an existing department.
     * A blank reference means "no department assigned" and is always accepted.
     */
    public void validateDepartmentExists(String departmentReference) {
        if (isBlank(departmentReference)) {
            return;
        }
        findDepartmentByReference(departmentReference);
    }

    /**
     * Resolves a department by id (numeric reference) or by code/name (any other reference).
     *
     * @return {@code null} when the reference is blank
     * @throws DepartmentReferenceNotFoundException  the department does not exist
     * @throws DepartmentServiceTimeoutException     the department service did not answer in time
     * @throws DepartmentServiceCommunicationException the department service could not be reached
     *                                                 or answered with an unexpected status
     */
    public DepartmentResponse findDepartmentByReference(String departmentReference) {
        if (isBlank(departmentReference)) {
            return null;
        }

        if (isNumericReference(departmentReference)) {
            return fetchDepartmentById(Long.parseLong(departmentReference));
        }

        return fetchDepartmentByReference(departmentReference);
    }

    private DepartmentResponse fetchDepartmentById(Long departmentId) {
        log.debug("Resolving department {} via service discovery at {}", departmentId, properties.getBaseUrl());
        DepartmentResponse department = execute(
                () -> restClient.get()
                        .uri(DEPARTMENT_BY_ID_PATH, departmentId)
                        .retrieve()
                        .body(DepartmentResponse.class),
                // A 404 on the by-id endpoint is the department service telling us the
                // department reference is invalid - a client error, not an outage.
                () -> new DepartmentReferenceNotFoundException(
                        "Department '" + departmentId + "' does not exist")
        );
        if (department == null) {
            throw new DepartmentServiceCommunicationException(
                    "Department service returned an empty body for department " + departmentId);
        }
        return department;
    }

    private DepartmentResponse fetchDepartmentByReference(String departmentReference) {
        log.debug("Resolving department {} via service discovery at {}", departmentReference, properties.getBaseUrl());
        DepartmentResponse department = execute(
                () -> restClient.get()
                        .uri(DEPARTMENTS_PATH + "/reference/{reference}", departmentReference)
                        .retrieve()
                        .body(DepartmentResponse.class),
                () -> new DepartmentReferenceNotFoundException(
                        "Department '" + departmentReference + "' does not exist")
        );
        if (department == null) {
            throw new DepartmentServiceCommunicationException(
                    "Department service returned an empty body for department " + departmentReference);
        }
        return department;
    }

    /**
     * Runs {@code call}, translating every failure mode of a load-balanced call into a
     * department-service exception.
     *
     * @param notFoundTranslation how to translate an HTTP 404 into a domain exception
     */
    private <T> T execute(Supplier<T> call, Supplier<RuntimeException> notFoundTranslation) {
        try {
            return call.get();
        } catch (ResourceAccessException ex) {
            if (containsCause(ex, SocketTimeoutException.class)) {
                Duration readTimeout = properties.getReadTimeout();
                throw new DepartmentServiceTimeoutException(
                        "Department service timed out after " + readTimeout.toMillis() + "ms", ex);
            }
            throw new DepartmentServiceCommunicationException("Could not reach department service", ex);
        } catch (HttpStatusCodeException ex) {
            if (notFoundTranslation != null && ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw notFoundTranslation.get();
            }
            throw new DepartmentServiceCommunicationException(
                    "Department service request failed with status " + ex.getStatusCode(), ex);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            // Spring Cloud LoadBalancer found no live instance for the logical service id,
            // e.g. the department service has not registered with Eureka (yet).
            if (isNoInstanceAvailable(ex)) {
                throw new DepartmentServiceCommunicationException(
                        "No department service instance is available from service discovery", ex);
            }
            throw ex;
        }
    }

    private boolean isNoInstanceAvailable(RuntimeException ex) {
        String message = ex.getMessage();
        return message != null && NO_INSTANCE_MESSAGES.stream().anyMatch(message::startsWith);
    }

    private boolean isBlank(String departmentReference) {
        return departmentReference == null || departmentReference.isBlank();
    }

    private boolean isNumericReference(String departmentReference) {
        return departmentReference.matches("\\d+");
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
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return requestFactory;
    }
}
