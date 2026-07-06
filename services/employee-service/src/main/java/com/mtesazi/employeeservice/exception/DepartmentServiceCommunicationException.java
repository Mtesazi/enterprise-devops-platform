package com.mtesazi.employeeservice.exception;

public class DepartmentServiceCommunicationException extends RuntimeException {

    public DepartmentServiceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DepartmentServiceCommunicationException(String message) {
        super(message);
    }

}
