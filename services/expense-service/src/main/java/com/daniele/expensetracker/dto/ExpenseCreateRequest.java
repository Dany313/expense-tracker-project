package com.daniele.expensetracker.dto;

import com.daniele.expensetracker.domain.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Payload per la creazione o la sostituzione completa di una spesa")
public record ExpenseCreateRequest(

        @NotNull(message = "La data della spesa è obbligatoria")
        @Schema(description = "Data in cui è avvenuta la spesa (ISO-8601 YYYY-MM-DD)", example = "2026-10-19")
        LocalDate expenseDate,

        @NotBlank(message = "Il nome del negozio è obbligatorio")
        @Size(max = 150, message = "Il nome del negozio non può superare i 150 caratteri")
        @Schema(description = "Nome dell'esercizio commerciale o fornitore", example = "CONAD")
        String storeName,

        @NotNull(message = "L'importo della spesa è obbligatorio")
        @Positive(message = "L'importo deve essere strettamente maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 10 cifre intere e 2 decimali")
        @Schema(description = "Importo della spesa", example = "42.80")
        BigDecimal amount,

        @NotNull(message = "La categoria della spesa è obbligatoria")
        @Schema(description = "Categoria principale della spesa", example = "SPESA_ALIMENTARE_E_CASA")
        ExpenseCategory category,

        @Size(max = 100, message = "La sottocategoria non può superare i 100 caratteri")
        @Schema(description = "Sottocategoria opzionale per un dettaglio più fine", example = "Spesa settimanale", nullable = true)
        String subcategory,

        @Size(max = 500, message = "La descrizione non può superare i 500 caratteri")
        @Schema(description = "Note o descrizione aggiuntiva della spesa", example = "Spesa alimentare settimanale al supermercato", nullable = true)
        String description,

        @Schema(description = "Indica se si tratta di una spesa ricorrente o abbonamento", example = "false", defaultValue = "false")
        Boolean recurring,

        @Schema(description = "Indica se la spesa è condivisa al 50% con qualcun altro", example = "false", defaultValue = "false")
        Boolean shared
) {
    public ExpenseCreateRequest(
            LocalDate expenseDate,
            String storeName,
            BigDecimal amount,
            ExpenseCategory category,
            String subcategory,
            String description,
            Boolean recurring
    ) {
        this(expenseDate, storeName, amount, category, subcategory, description, recurring, false);
    }
}
