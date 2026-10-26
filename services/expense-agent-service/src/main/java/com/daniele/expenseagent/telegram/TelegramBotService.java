package com.daniele.expenseagent.telegram;

import com.daniele.expenseagent.dto.AgentInteractionResponse;
import com.daniele.expenseagent.dto.AgentJobStatusResponse;
import com.daniele.expenseagent.dto.AgentTextRequest;
import com.daniele.expenseagent.dto.AgentVoiceBase64Request;
import com.daniele.expenseagent.exception.UnauthorizedTelegramAccessException;
import com.daniele.expenseagent.messaging.ExpenseAgentQueueService;
import com.daniele.expenseagent.service.ExpenseAgentOrchestratorService;
import com.daniele.expenseagent.telegram.TelegramUpdate.TelegramMessage;
import com.daniele.expenseagent.telegram.TelegramUpdate.TelegramPhotoSize;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TelegramBotService {

    public static final String TELEGRAM_SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final ExpenseAgentOrchestratorService orchestratorService;
    private final ExpenseAgentQueueService queueService;
    private final TelegramBotGateway telegramBotGateway;
    private final String expectedWebhookSecret;
    private final Set<Long> allowedUserIds;
    private final boolean asyncQueueMode;

    public TelegramBotService(
            ExpenseAgentOrchestratorService orchestratorService,
            ExpenseAgentQueueService queueService,
            TelegramBotGateway telegramBotGateway,
            @Value("${telegram.bot.webhook-secret:}") String expectedWebhookSecret,
            @Value("${telegram.bot.allowed-user-ids:}") String allowedUserIdsRaw,
            @Value("${telegram.bot.async-queue-mode:false}") boolean asyncQueueMode
    ) {
        this.orchestratorService = orchestratorService;
        this.queueService = queueService;
        this.telegramBotGateway = telegramBotGateway;
        this.expectedWebhookSecret = expectedWebhookSecret != null ? expectedWebhookSecret.trim() : "";
        this.allowedUserIds = parseAllowedUserIds(allowedUserIdsRaw);
        this.asyncQueueMode = asyncQueueMode;
    }

    public TelegramWebhookResponse handleWebhook(TelegramUpdate update, String providedWebhookSecret) {
        verifyWebhookSecret(providedWebhookSecret);

        if (update == null || update.message() == null) {
            return new TelegramWebhookResponse(false, null, "IGNORED", null, "Update privo di messaggio", Instant.now());
        }

        TelegramMessage message = update.message();
        Long senderId = message.from() != null ? message.from().id() : null;
        String senderUsername = message.from() != null ? message.from().username() : "unknown";
        Long chatId = message.chat() != null ? message.chat().id() : senderId;

        // Comando di utilità /id o /whoami per leggere il proprio Telegram User ID durante il primo setup
        if (message.text() != null && (message.text().trim().equalsIgnoreCase("/id")
                || message.text().trim().equalsIgnoreCase("/whoami"))) {
            if (!allowedUserIds.isEmpty() && (senderId == null || !allowedUserIds.contains(senderId))) {
                throw new UnauthorizedTelegramAccessException(
                        HttpStatus.FORBIDDEN,
                        "L'utente Telegram " + senderId + " non è autorizzato ad utilizzare questo bot personale"
                );
            }
            String reply = "Il tuo Telegram User ID è: " + senderId;
            telegramBotGateway.sendChatMessage(chatId, reply);
            return new TelegramWebhookResponse(true, senderId, "TEXT", null, reply, Instant.now());
        }

        verifyAllowedTelegramUser(senderId, senderUsername);

        // 1. Messaggio Vocale (.ogg da Telegram)
        if (message.voice() != null && message.voice().fileId() != null) {
            byte[] audioBytes = telegramBotGateway.downloadTelegramFile(message.voice().fileId());
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            String mimeType = message.voice().mimeType() != null ? message.voice().mimeType() : "audio/ogg";

            if (asyncQueueMode) {
                AgentJobStatusResponse job = queueService.enqueueVoiceBase64(new AgentVoiceBase64Request(base64Audio, mimeType));
                String reply = "🎙️ Messaggio vocale ricevuto e messo in coda (Job ID: " + job.jobId() + ").";
                telegramBotGateway.sendChatMessage(chatId, reply);
                return new TelegramWebhookResponse(true, senderId, "VOICE", job.jobId(), reply, Instant.now());
            }

            AgentInteractionResponse response = orchestratorService.processVoiceBase64(
                    new AgentVoiceBase64Request(base64Audio, mimeType)
            );
            telegramBotGateway.sendChatMessage(chatId, response.agentResponse());
            return new TelegramWebhookResponse(true, senderId, "VOICE", null, response.agentResponse(), Instant.now());
        }

        // 2. Foto di uno Scontrino (inviata a receipt-ocr-service con token M2M e salvataggio su coda RabbitMQ)
        List<TelegramPhotoSize> photos = message.photo();
        if (photos != null && !photos.isEmpty()) {
            TelegramPhotoSize largestPhoto = photos.stream()
                    .max(Comparator.comparingInt(p -> p.fileSize() != null ? p.fileSize() : 0))
                    .orElse(photos.getLast());

            byte[] photoBytes = telegramBotGateway.downloadTelegramFile(largestPhoto.fileId());
            String base64Image = Base64.getEncoder().encodeToString(photoBytes);
            String ocrJobId = telegramBotGateway.enqueueReceiptPhotoOcr(base64Image, "image/jpeg");

            String reply = "🧾 Scontrino ricevuto! Analisi OCR e salvataggio spesa avviati su coda RabbitMQ (Job: " + ocrJobId + ").";
            telegramBotGateway.sendChatMessage(chatId, reply);
            return new TelegramWebhookResponse(true, senderId, "PHOTO_RECEIPT", null, reply, Instant.now());
        }

        // 3. Messaggio di Testo
        if (message.text() != null && !message.text().isBlank()) {
            String cleanText = message.text().trim();
            if (asyncQueueMode) {
                AgentJobStatusResponse job = queueService.enqueueText(new AgentTextRequest(cleanText));
                String reply = "✅ Richiesta ricevuta e messa in coda (Job ID: " + job.jobId() + ").";
                telegramBotGateway.sendChatMessage(chatId, reply);
                return new TelegramWebhookResponse(true, senderId, "TEXT", job.jobId(), reply, Instant.now());
            }

            AgentInteractionResponse response = orchestratorService.processText(new AgentTextRequest(cleanText));
            telegramBotGateway.sendChatMessage(chatId, response.agentResponse());
            return new TelegramWebhookResponse(true, senderId, "TEXT", null, response.agentResponse(), Instant.now());
        }

        return new TelegramWebhookResponse(false, senderId, "IGNORED", null, "Formato messaggio Telegram non supportato", Instant.now());
    }

    private void verifyWebhookSecret(String providedSecret) {
        if (expectedWebhookSecret.isBlank()) {
            return;
        }
        if (providedSecret == null || providedSecret.isBlank()) {
            throw new UnauthorizedTelegramAccessException(
                    HttpStatus.UNAUTHORIZED,
                    "Header X-Telegram-Bot-Api-Secret-Token mancante"
            );
        }
        boolean valid = MessageDigest.isEqual(
                providedSecret.trim().getBytes(StandardCharsets.UTF_8),
                expectedWebhookSecret.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            throw new UnauthorizedTelegramAccessException(
                    HttpStatus.UNAUTHORIZED,
                    "Header X-Telegram-Bot-Api-Secret-Token non valido"
            );
        }
    }

    private void verifyAllowedTelegramUser(Long senderId, String username) {
        if (senderId == null) {
            throw new UnauthorizedTelegramAccessException(
                    HttpStatus.FORBIDDEN,
                    "Impossibile identificare il Telegram User ID del mittente"
            );
        }
        if (allowedUserIds.isEmpty() || !allowedUserIds.contains(senderId)) {
            log.warn("Tentativo di accesso non autorizzato al Bot Telegram da telegramUserId={} (@{}). Whitelist configurata: {}",
                    senderId, username, allowedUserIds);
            throw new UnauthorizedTelegramAccessException(
                    HttpStatus.FORBIDDEN,
                    "L'utente Telegram " + senderId + " non è autorizzato ad utilizzare questo bot personale"
            );
        }
    }

    private static Set<Long> parseAllowedUserIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toUnmodifiableSet());
    }
}
