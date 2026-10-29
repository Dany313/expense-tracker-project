package com.daniele.receiptocr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dati della spesa estratti dallo scontrino, direttamente compatibili con il payload di creazione spesa di expense-service")
public record ReceiptExtractionResponse(

        @Schema(description = "Data dello scontrino (YYYY-MM-DD)", example = "2026-10-19")
        LocalDate expenseDate,

        @Schema(description = "Nome normalizzato del negozio estratto dallo scontrino", example = "CONAD")
        String storeName,

        @Schema(description = "Importo totale pagato", example = "38.50")
        BigDecimal amount,

        @Schema(description = "Categoria principale suggerita (compatibile con expense-service)", example = "SPESA_ALIMENTARE_E_CASA")
        String category,

        @Schema(description = "Sottocategoria opzionale suggerita", example = "Spesa supermercato", nullable = true)
        String subcategory,

        @Schema(description = "Descrizione sintetica opzionale dello scontrino", example = "Scontrino CONAD del 2026-10-19", nullable = true)
        String description,

        @Schema(description = "Indica se è una spesa ricorrente (default false per gli scontrini)", example = "false")
        Boolean recurring
) {
}
