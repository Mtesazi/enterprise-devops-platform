package com.mtesazi.employeeservice.service;

import com.mtesazi.employeeservice.client.DepartmentClient;
import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.exception.DepartmentReferenceNotFoundException;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import com.mtesazi.employeeservice.exception.DepartmentServiceTimeoutException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentLookupService {

    private final DepartmentClient departmentClient;

    @CircuitBreaker(name = "departmentService", fallbackMethod = "validateDepartmentExistsFallback")
    @Retry(name = "departmentService", fallbackMethod = "validateDepartmentExistsFallback")
    public void validateDepartmentExists(String departmentReference) {
        departmentClient.validateDepartmentExists(departmentReference);
    }

    @CircuitBreaker(name = "departmentService", fallbackMethod = "findDepartmentByReferenceFallback")
    @Retry(name = "departmentService", fallbackMethod = "findDepartmentByReferenceFallback")
    public DepartmentResponse findDepartmentByReference(String departmentReference) {
        return departmentClient.findDepartmentByReference(departmentReference);
    }

    @SuppressWarnings("unused")
    DepartmentResponse findDepartmentByReferenceFallback(String departmentReference, Throwable throwable) {
        throw mapDepartmentFailure(throwable);
    }

    @SuppressWarnings("unused")
    void validateDepartmentExistsFallback(String departmentReference, Throwable throwable) {
        throw mapDepartmentFailure(throwable);
    }

    private RuntimeException mapDepartmentFailure(Throwable throwable) {
        DepartmentReferenceNotFoundException departmentReferenceNotFoundException =
                findCause(throwable, DepartmentReferenceNotFoundException.class);
        if (departmentReferenceNotFoundException != null) {
            return departmentReferenceNotFoundException;
        }

        DepartmentServiceTimeoutException departmentServiceTimeoutException =
                findCause(throwable, DepartmentServiceTimeoutException.class);
        if (departmentServiceTimeoutException != null) {
            return departmentServiceTimeoutException;
        }

        DepartmentServiceCommunicationException departmentServiceCommunicationException =
                findCause(throwable, DepartmentServiceCommunicationException.class);
        if (departmentServiceCommunicationException != null) {
            return departmentServiceCommunicationException;
        }

        CallNotPermittedException callNotPermittedException =
                findCause(throwable, CallNotPermittedException.class);
        if (callNotPermittedException != null) {
            return new DepartmentServiceCommunicationException("Department service circuit breaker is open", callNotPermittedException);
        }

        Throwable rootCause = unwrapCause(throwable);
        return new DepartmentServiceCommunicationException("Department service request failed", rootCause);
    }

    private Throwable unwrapCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
