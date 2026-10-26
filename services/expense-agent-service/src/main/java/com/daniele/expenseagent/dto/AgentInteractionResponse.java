package com.daniele.expenseagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Risposta restituita dall'agente AI dopo la trascrizione (se vocale) e l'interazione via MCP con expense-service")
public record AgentInteractionResponse(

        @Schema(description = "Tipo di input ricevuto (TEXT o VOICE)", example = "VOICE")
        InputType inputType,

        @Schema(description = "Testo inviato (o trascritto dal vocale) e inoltrato a Gemini", example = "Ho speso 32 euro e 50 alla Conad per la spesa alimentare")
        String processedText,

        @Schema(description = "Risposta elaborata da Gemini dopo aver eseguito i tool MCP sul backend", example = "Ho registrato la tua spesa di 32,50 EUR presso CONAD nella categoria GROCERIES.")
        String agentResponse,

        @Schema(description = "Timestamp dell'elaborazione")
        Instant timestamp
) {
}
