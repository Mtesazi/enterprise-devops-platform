package com.mtesazi.employeeservice.service;

import com.mtesazi.employeeservice.client.DepartmentClient;
import com.mtesazi.employeeservice.client.dto.DepartmentResponse;
import com.mtesazi.employeeservice.exception.DepartmentReferenceNotFoundException;
import com.mtesazi.employeeservice.exception.DepartmentServiceCommunicationException;
import com.mtesazi.employeeservice.exception.DepartmentServiceTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentLookupServiceTest {

    @Mock
    private DepartmentClient departmentClient;

    @InjectMocks
    private DepartmentLookupService departmentLookupService;

    @Test
    void findDepartmentByReferenceDelegatesToClient() {
        DepartmentResponse response = new DepartmentResponse(10L, "Engineering", "ENG", "Engineering department");

        when(departmentClient.findDepartmentByReference("ENG")).thenReturn(response);

        DepartmentResponse result = departmentLookupService.findDepartmentByReference("ENG");

        assertSame(response, result);
        verify(departmentClient).findDepartmentByReference("ENG");
    }

    @Test
    void fallbackPropagatesTimeoutExceptions() {
        DepartmentServiceTimeoutException timeoutException =
                new DepartmentServiceTimeoutException("Department service timed out after 3000ms", new RuntimeException("timeout"));

        DepartmentResponse response = departmentLookupService.findDepartmentByReferenceFallback("ENG", timeoutException);

        assertEquals("Department Service Unavailable", response.name());
        assertEquals("N/A", response.code());
    }

    @Test
    void fallbackPropagatesDepartmentReferenceErrors() {
        DepartmentReferenceNotFoundException cause =
                new DepartmentReferenceNotFoundException("Department 'ENG' does not exist");

        DepartmentReferenceNotFoundException exception = assertThrows(
                DepartmentReferenceNotFoundException.class,
                () -> departmentLookupService.validateDepartmentExistsFallback("ENG", cause)
        );

        assertSame(cause, exception);
    }

    @Test
    void fallbackWrapsUnexpectedFailures() {
        IllegalStateException cause = new IllegalStateException("boom");

        DepartmentResponse response = departmentLookupService.findDepartmentByReferenceFallback("ENG", cause);

        assertEquals("Department Service Unavailable", response.name());
        assertEquals("N/A", response.code());
    }
}
