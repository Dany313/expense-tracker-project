package com.daniele.receiptocr.service;

import com.daniele.receiptocr.dto.ReceiptBase64Request;
import com.daniele.receiptocr.dto.ReceiptExtractionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ReceiptOcrService {

    ReceiptExtractionResponse extractFromMultipart(MultipartFile file);

    ReceiptExtractionResponse extractFromBase64(ReceiptBase64Request request);

    ReceiptExtractionResponse extractFromBytes(byte[] imageBytes, String contentType);
}
