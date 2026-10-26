package com.daniele.receiptocr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Stato di avanzamento e risultato di un job OCR scontrino accodato su RabbitMQ")
public record ReceiptOcrJobStatusResponse(
        @Schema(description = "ID univoco del job OCR")
        UUID jobId,

        @Schema(description = "Stato corrente del job (QUEUED, PROCESSING, COMPLETED, FAILED_DLQ)")
        ReceiptOcrJobStatus status,

        @Schema(description = "Se true, una volta estratti i dati dallo scontrino, la spesa viene pubblicata sulla coda expense.create.queue per il salvataggio automatico in expense-service")
        boolean autoSaveExpense,

        @Schema(description = "Dati estratti dallo scontrino (valorizzato quando status = COMPLETED)")
        ReceiptExtractionResponse extractedExpense,

        @Schema(description = "Messaggio descrittivo o eventuale errore")
        String message,

        @Schema(description = "Timestamp di creazione del job")
        Instant createdAt,

        @Schema(description = "Timestamp di ultimo aggiornamento del job")
        Instant updatedAt
) {
}
