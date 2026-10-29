package com.daniele.expensetracker.dto;

import com.daniele.expensetracker.domain.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Payload per l'aggiornamento parziale (PATCH) di una spesa esistente")
public record ExpenseUpdateRequest(

        @Schema(description = "Nuova data della spesa (opzionale)", example = "2026-10-19")
        LocalDate expenseDate,

        @Size(max = 150, message = "Il nome del negozio non può superare i 150 caratteri")
        @Schema(description = "Nuovo nome del negozio (opzionale)", example = "ESSELUNGA")
        String storeName,

        @Positive(message = "L'importo deve essere strettamente maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 10 cifre intere e 2 decimali")
        @Schema(description = "Nuovo importo della spesa (opzionale)", example = "55.20")
        BigDecimal amount,

        @Schema(description = "Nuova categoria della spesa (opzionale)", example = "SPESA_ALIMENTARE_E_CASA")
        ExpenseCategory category,

        @Size(max = 100, message = "La sottocategoria non può superare i 100 caratteri")
        @Schema(description = "Nuova sottocategoria (opzionale)", example = "Ortofrutta")
        String subcategory,

        @Size(max = 500, message = "La descrizione non può superare i 500 caratteri")
        @Schema(description = "Nuova descrizione o nota (opzionale)", example = "Spesa con sconto fedeltà")
        String description,

        @Schema(description = "Aggiorna flag spesa ricorrente (opzionale)", example = "false")
        Boolean recurring,

        @Schema(description = "Aggiorna flag spesa condivisa al 50% (opzionale)", example = "false")
        Boolean shared
) {
    public ExpenseUpdateRequest(
            LocalDate expenseDate,
            String storeName,
            BigDecimal amount,
            ExpenseCategory category,
            String subcategory,
            String description,
            Boolean recurring
    ) {
        this(expenseDate, storeName, amount, category, subcategory, description, recurring, null);
    }
}
