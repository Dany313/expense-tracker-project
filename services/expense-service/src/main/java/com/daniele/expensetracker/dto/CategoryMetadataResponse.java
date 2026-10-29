package com.daniele.expensetracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadati di una categoria di spesa registrata nel database")
public record CategoryMetadataResponse(
        @Schema(description = "Codice univoco della categoria (in maiuscolo con underscore)", example = "USCITE_FUORI")
        String code,

        @Schema(description = "Nome leggibile della categoria per la UI", example = "Uscite fuori")
        String displayName,

        @Schema(description = "Sinonimi e parole chiave per la categorizzazione automatica (separati da virgola)", example = "ristorante, pizzeria, bar, aperitivo")
        String synonyms
) {
    public CategoryMetadataResponse(String code, String displayName) {
        this(code, displayName, null);
    }
}
