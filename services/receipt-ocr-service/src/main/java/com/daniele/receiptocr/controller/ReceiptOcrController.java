package com.daniele.receiptocr.controller;

import com.daniele.receiptocr.dto.ReceiptBase64Request;
import com.daniele.receiptocr.dto.ReceiptExtractionResponse;
import com.daniele.receiptocr.dto.ReceiptOcrJobStatusResponse;
import com.daniele.receiptocr.messaging.ReceiptOcrQueueService;
import com.daniele.receiptocr.service.ReceiptOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ocr/receipts")
@Tag(name = "Receipt OCR", description = "Estrazione AI dei dati di spesa, negozio e data dalle foto degli scontrini")
public class ReceiptOcrController {

    private final ReceiptOcrService receiptOcrService;
    private final ReceiptOcrQueueService receiptOcrQueueService;

    public ReceiptOcrController(ReceiptOcrService receiptOcrService, ReceiptOcrQueueService receiptOcrQueueService) {
        this.receiptOcrService = receiptOcrService;
        this.receiptOcrQueueService = receiptOcrQueueService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Estrae i dati della spesa da un file immagine (Multipart)",
            description = "Analizza la foto dello scontrino tramite Vision AI e restituisce negozio, data e dati della spesa in un formato pronto per essere inviato a expense-service."
    )
    public ResponseEntity<ReceiptExtractionResponse> extractFromMultipart(
            @Parameter(description = "File immagine dello scontrino (JPEG, PNG, WEBP, HEIC)")
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(receiptOcrService.extractFromMultipart(file));
    }

    @PostMapping(path = "/base64", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Estrae i dati della spesa da un'immagine codificata in Base64",
            description = "Analizza uno scontrino codificato in Base64 e restituisce negozio, data e dati della spesa."
    )
    public ResponseEntity<ReceiptExtractionResponse> extractFromBase64(
            @Valid @RequestBody ReceiptBase64Request request
    ) {
        return ResponseEntity.ok(receiptOcrService.extractFromBase64(request));
    }

    @PostMapping(path = "/enqueue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Accoda l'analisi OCR di uno scontrino (Multipart) su RabbitMQ",
            description = "Accoda lo scontrino su RabbitMQ per l'elaborazione asincrona resiliente a picchi di Gemini API. Se autoSaveExpense=true, i dati estratti vengono inoltrati automaticamente sulla coda expense.create.queue per il salvataggio in expense-service."
    )
    public ResponseEntity<ReceiptOcrJobStatusResponse> enqueueFromMultipart(
            @Parameter(description = "File immagine dello scontrino")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Se true (default), inoltra i dati estratti alla coda expense.create.queue di expense-service")
            @RequestParam(name = "autoSaveExpense", defaultValue = "true") boolean autoSaveExpense
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(receiptOcrQueueService.enqueueMultipart(file, autoSaveExpense));
    }

    @PostMapping(path = "/base64/enqueue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Accoda l'analisi OCR di uno scontrino (Base64) su RabbitMQ",
            description = "Accoda lo scontrino codificato in Base64 su RabbitMQ con retry esponenziale automatico e Dead Letter Queue."
    )
    public ResponseEntity<ReceiptOcrJobStatusResponse> enqueueFromBase64(
            @Valid @RequestBody ReceiptBase64Request request,
            @Parameter(description = "Se true (default), inoltra i dati estratti alla coda expense.create.queue di expense-service")
            @RequestParam(name = "autoSaveExpense", defaultValue = "true") boolean autoSaveExpense
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(receiptOcrQueueService.enqueueBase64(request, autoSaveExpense));
    }

    @GetMapping(path = "/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Controlla lo stato di un job OCR scontrino accodato su RabbitMQ",
            description = "Restituisce lo stato corrente (QUEUED, PROCESSING, COMPLETED, FAILED_DLQ) e i dati estratti quando completato."
    )
    public ResponseEntity<ReceiptOcrJobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(receiptOcrQueueService.getJobStatus(jobId));
    }
}
