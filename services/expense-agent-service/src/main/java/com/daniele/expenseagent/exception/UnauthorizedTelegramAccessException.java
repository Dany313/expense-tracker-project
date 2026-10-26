package com.daniele.expenseagent.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedTelegramAccessException extends RuntimeException {

    private final HttpStatus status;

    public UnauthorizedTelegramAccessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
