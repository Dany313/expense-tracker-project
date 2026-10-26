package com.daniele.receiptocr.service;

import java.math.BigDecimal;
import org.springframework.util.MimeType;

public interface ReceiptVisionExtractor {

    RawVisionReceiptResult extractFromImage(byte[] imageBytes, MimeType mimeType);

    record RawVisionReceiptResult(
            Boolean validReceipt,
            String expenseDate,
            String storeName,
            BigDecimal amount,
            String category,
            String subcategory,
            String description,
            String errorMessage
    ) {
    }
}
