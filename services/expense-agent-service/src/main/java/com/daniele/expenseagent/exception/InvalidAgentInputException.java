package com.daniele.expenseagent.exception;

public class InvalidAgentInputException extends RuntimeException {

    public InvalidAgentInputException(String message) {
        super(message);
    }

    public InvalidAgentInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
