package com.daniele.receiptocr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Richiesta JSON con immagine dello scontrino codificata in Base64")
public record ReceiptBase64Request(

        @NotBlank(message = "L'immagine in Base64 è obbligatoria")
        @Schema(description = "Contenuto binario dell'immagine codificato in Base64 (senza prefisso data URI oppure con prefisso)")
        String base64Image,

        @Schema(description = "MIME type dell'immagine (es. image/jpeg, image/png, image/webp). Default: image/jpeg", example = "image/jpeg", defaultValue = "image/jpeg")
        String mimeType
) {
}
