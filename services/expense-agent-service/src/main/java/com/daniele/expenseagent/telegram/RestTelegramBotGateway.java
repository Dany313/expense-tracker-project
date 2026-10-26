package com.daniele.expenseagent.telegram;

import com.daniele.expenseagent.security.InternalM2mSecurityFilter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestTelegramBotGateway implements TelegramBotGateway {

    private static final Logger log = LoggerFactory.getLogger(RestTelegramBotGateway.class);

    private final RestClient restClient;
    private final String botToken;
    private final String receiptOcrBaseUrl;
    private final String m2mSecret;

    public RestTelegramBotGateway(
            @Value("${telegram.bot.token:}") String botToken,
            @Value("${receipt.ocr.service.url:http://localhost:8081}") String receiptOcrBaseUrl,
            @Value("${security.m2m.secret:expense-tracker-internal-m2m-secret-change-me}") String m2mSecret
    ) {
        this.restClient = RestClient.builder().build();
        this.botToken = botToken != null ? botToken.trim() : "";
        this.receiptOcrBaseUrl = receiptOcrBaseUrl;
        this.m2mSecret = m2mSecret != null ? m2mSecret.trim() : "";
    }

    @Override
    public byte[] downloadTelegramFile(String fileId) {
        if (botToken.isBlank()) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN non configurato: impossibile scaricare il file da Telegram");
        }
        String getFileUrl = "https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId;
        TelegramGetFileResponse fileResponse = restClient.get()
                .uri(getFileUrl)
                .retrieve()
                .body(TelegramGetFileResponse.class);

        if (fileResponse == null || !fileResponse.ok() || fileResponse.result() == null || fileResponse.result().filePath() == null) {
            throw new IllegalStateException("Impossibile ottenere il percorso del file da Telegram per file_id=" + fileId);
        }

        String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + fileResponse.result().filePath();
        return restClient.get()
                .uri(downloadUrl)
                .retrieve()
                .body(byte[].class);
    }

    @Override
    public void sendChatMessage(Long chatId, String text) {
        if (botToken.isBlank() || chatId == null || text == null || text.isBlank()) {
            log.debug("Invio messaggio Telegram saltato (token non configurato o chatId nullo). Messaggio: {}", text);
            return;
        }
        try {
            String sendMessageUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            restClient.post()
                    .uri(sendMessageUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", text
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Errore nell'invio della risposta al chatId Telegram {}: {}", chatId, ex.getMessage());
        }
    }

    @Override
    public String enqueueReceiptPhotoOcr(String base64Image, String mimeType) {
        String url = receiptOcrBaseUrl + "/api/v1/ocr/receipts/base64/enqueue?autoSaveExpense=true";
        OcrEnqueueResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header(InternalM2mSecurityFilter.M2M_HEADER_NAME, m2mSecret)
                .body(Map.of(
                        "base64Image", base64Image,
                        "mimeType", mimeType != null ? mimeType : "image/jpeg"
                ))
                .retrieve()
                .body(OcrEnqueueResponse.class);

        return response != null && response.jobId() != null ? response.jobId() : "unknown";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramGetFileResponse(
            @JsonProperty("ok") boolean ok,
            @JsonProperty("result") TelegramFileResult result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramFileResult(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("file_path") String filePath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OcrEnqueueResponse(
            @JsonProperty("jobId") String jobId,
            @JsonProperty("status") String status
    ) {
    }
}
