package com.daniele.expensetracker.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forExpenseId(UUID id) {
        return new ResourceNotFoundException("Spesa non trovata con id: " + id);
    }
}
