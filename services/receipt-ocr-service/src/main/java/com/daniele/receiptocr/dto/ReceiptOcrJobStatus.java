package com.daniele.receiptocr.dto;

public enum ReceiptOcrJobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED_DLQ
}
