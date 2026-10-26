package com.daniele.expenseagent.telegram;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telegram")
@Tag(name = "Telegram Bot Webhook", description = "Webhook protetto per il Bot Telegram personale (testo, vocali .ogg e foto scontrini) con filtro su Secret Token e Whitelist Telegram User ID")
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;

    public TelegramWebhookController(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostMapping(path = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Riceve gli update dal Bot Telegram personale",
            description = "Verifica X-Telegram-Bot-Api-Secret-Token e controlla che message.from.id appartenga alla whitelist TELEGRAM_ALLOWED_USER_IDS prima di elaborare testi, vocali o foto di scontrini."
    )
    public ResponseEntity<TelegramWebhookResponse> onTelegramUpdate(
            @RequestBody TelegramUpdate update,
            @RequestHeader(name = TelegramBotService.TELEGRAM_SECRET_HEADER, required = false) String secretToken
    ) {
        return ResponseEntity.ok(telegramBotService.handleWebhook(update, secretToken));
    }
}
