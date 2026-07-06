package com.mtesazi.employeeservice.exception;

public class DepartmentServiceTimeoutException extends RuntimeException {

    public DepartmentServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
