package com.daniele.expenseagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Richiesta testuale da inviare all'agente AI")
public record AgentTextRequest(

        @NotBlank(message = "Il testo del messaggio non può essere vuoto")
        @Size(max = 4000, message = "Il testo non può superare i 4000 caratteri")
        @Schema(
                description = "Messaggio o comando in linguaggio naturale (o testo di una notifica)",
                example = "Oggi ho speso 32,50 euro alla Conad per la spesa settimanale con carta"
        )
        String text
) {
}
