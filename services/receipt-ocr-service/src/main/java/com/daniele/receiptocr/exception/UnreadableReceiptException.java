package com.daniele.receiptocr.exception;

public class UnreadableReceiptException extends RuntimeException {

    public UnreadableReceiptException(String message) {
        super(message);
    }

    public UnreadableReceiptException(String message, Throwable cause) {
        super(message, cause);
    }
}
