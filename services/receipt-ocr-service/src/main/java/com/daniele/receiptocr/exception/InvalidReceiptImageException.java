package com.daniele.receiptocr.exception;

public class InvalidReceiptImageException extends RuntimeException {

    public InvalidReceiptImageException(String message) {
        super(message);
    }

    public InvalidReceiptImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
