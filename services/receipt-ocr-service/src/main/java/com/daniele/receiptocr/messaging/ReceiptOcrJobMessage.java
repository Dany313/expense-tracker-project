package com.daniele.receiptocr.messaging;

import java.time.Instant;
import java.util.UUID;

public record ReceiptOcrJobMessage(
        UUID jobId,
        String base64Image,
        String mimeType,
        boolean autoSaveExpense,
        Instant createdAt
) {
}
