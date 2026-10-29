package com.daniele.expensetracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Richiesta per la creazione o modifica di una categoria a database")
public record CategoryRequest(
        @NotBlank(message = "Il codice della categoria è obbligatorio")
        @Size(max = 50, message = "Il codice non può superare 50 caratteri")
        @Schema(description = "Codice univoco (es. ANIMALI)", example = "ANIMALI")
        String code,

        @NotBlank(message = "Il nome visualizzato è obbligatorio")
        @Size(max = 100, message = "Il nome visualizzato non può superare 100 caratteri")
        @Schema(description = "Nome visualizzato nella UI", example = "Animali e veterinario")
        String displayName,

        @Size(max = 500, message = "I sinonimi non possono superare 500 caratteri")
        @Schema(description = "Sinonimi opzionali separati da virgola", example = "veterinario, cibo gatti, crocchette, cane")
        String synonyms
) {
}
