package com.daniele.receiptocr.exception;

import java.util.UUID;

public class OcrJobNotFoundException extends RuntimeException {

    public OcrJobNotFoundException(UUID jobId) {
        super("Job OCR scontrino non trovato con id: " + jobId);
    }
}
