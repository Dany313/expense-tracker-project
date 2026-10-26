package com.daniele.expenseagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Stato di avanzamento e risultato di un comando testuale o vocale accodato su RabbitMQ per l'Agente AI")
public record AgentJobStatusResponse(
        @Schema(description = "ID univoco del job dell'Agente AI")
        UUID jobId,

        @Schema(description = "Tipo di input: TEXT oppure VOICE")
        InputType inputType,

        @Schema(description = "Stato corrente del job (QUEUED, PROCESSING, COMPLETED, FAILED_DLQ)")
        AgentJobStatus status,

        @Schema(description = "Risultato dell'interazione con Gemini connesso all'MCP (valorizzato quando status = COMPLETED)")
        AgentInteractionResponse result,

        @Schema(description = "Messaggio descrittivo o eventuale errore")
        String message,

        @Schema(description = "Timestamp di creazione del job")
        Instant createdAt,

        @Schema(description = "Timestamp di ultimo aggiornamento del job")
        Instant updatedAt
) {
}
