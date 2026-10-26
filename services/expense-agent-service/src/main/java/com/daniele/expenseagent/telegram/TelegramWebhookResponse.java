package com.daniele.expenseagent.telegram;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Esito dell'elaborazione di un messaggio proveniente dal Bot Telegram")
public record TelegramWebhookResponse(
        @Schema(description = "Se il messaggio è stato accettato ed elaborato")
        boolean accepted,

        @Schema(description = "ID utente Telegram del mittente")
        Long telegramUserId,

        @Schema(description = "Tipo di contenuto elaborato (TEXT, VOICE, PHOTO_RECEIPT, IGNORED)")
        String messageType,

        @Schema(description = "ID del job RabbitMQ se accodato in modalità asincrona")
        UUID queuedJobId,

        @Schema(description = "Risposta testuale inviata all'utente su Telegram")
        String replyText,

        @Schema(description = "Timestamp dell'elaborazione")
        Instant timestamp
) {
}
