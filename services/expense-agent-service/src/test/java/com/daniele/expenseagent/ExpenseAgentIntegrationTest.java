package com.daniele.expenseagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daniele.expenseagent.dto.AgentJobStatus;
import com.daniele.expenseagent.dto.AgentJobStatusResponse;
import com.daniele.expenseagent.dto.AgentTextRequest;
import com.daniele.expenseagent.messaging.ExpenseAgentQueueService;
import com.daniele.expenseagent.service.ExpenseAgentAiGateway;
import com.daniele.expenseagent.service.VoiceTranscriptionService;
import com.daniele.expenseagent.telegram.TelegramBotGateway;
import com.daniele.expenseagent.telegram.TelegramBotService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "security.m2m.enabled=false",
        "spring.ai.mcp.client.enabled=false",
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
        "spring.rabbitmq.listener.simple.retry.multiplier=1.5",
        "spring.rabbitmq.listener.simple.retry.max-attempts=3",
        "telegram.bot.webhook-secret=my-telegram-webhook-secret",
        "telegram.bot.allowed-user-ids=777888999"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExpenseAgentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseAgentQueueService expenseAgentQueueService;

    @MockitoBean
    private VoiceTranscriptionService voiceTranscriptionService;

    @MockitoBean
    private ExpenseAgentAiGateway expenseAgentAiGateway;

    @MockitoBean
    private TelegramBotGateway telegramBotGateway;

    @Test
    @DisplayName("REST /api/v1/agent/text: Invia testo a Gemini connesso all'MCP e restituisce la risposta")
    void shouldProcessTextMessageThroughGeminiMcpAgent() throws Exception {
        String userText = "Ho appena speso 28,50 euro da Aldi per la spesa alimentare";
        String expectedReply = "Ho registrato la spesa di 28,50 EUR presso ALDI nella categoria SPESA_ALIMENTARE_E_CASA.";

        when(expenseAgentAiGateway.executeWithMcpTools(userText)).thenReturn(expectedReply);

        String payload = """
                {
                  "text": "%s"
                }
                """.formatted(userText);

        mockMvc.perform(post("/api/v1/agent/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputType").value("TEXT"))
                .andExpect(jsonPath("$.processedText").value(userText))
                .andExpect(jsonPath("$.agentResponse").value(expectedReply))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(expenseAgentAiGateway).executeWithMcpTools(userText);
    }

    @Test
    @DisplayName("REST /api/v1/agent/voice: Trascrive un vocale (.ogg Telegram) in testo e lo manda a Gemini connesso all'MCP")
    void shouldTranscribeMultipartVoiceAndExecuteGeminiMcpAgent() throws Exception {
        String transcribedText = "Segna 15 euro spesi oggi da Tigotà per detersivi";
        String expectedReply = "Perfetto! Ho inserito la spesa di 15,00 EUR presso TIGOTA nella categoria SPESA_ALIMENTARE_E_CASA.";

        when(voiceTranscriptionService.transcribeAudioToText(any(), any())).thenReturn(transcribedText);
        when(expenseAgentAiGateway.executeWithMcpTools(eq(transcribedText))).thenReturn(expectedReply);

        MockMultipartFile voiceNote = new MockMultipartFile(
                "file",
                "telegram_voice.ogg",
                "audio/ogg",
                "fake-ogg-opus-voice-bytes".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/agent/voice").file(voiceNote))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputType").value("VOICE"))
                .andExpect(jsonPath("$.processedText").value(transcribedText))
                .andExpect(jsonPath("$.agentResponse").value(expectedReply))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(voiceTranscriptionService).transcribeAudioToText(any(), any());
        verify(expenseAgentAiGateway).executeWithMcpTools(transcribedText);
    }

    @Test
    @DisplayName("REST /api/v1/agent/voice/base64: Supporta audio in Base64 e restituisce 400 ProblemDetail per formato non valido")
    void shouldSupportBase64VoiceAndRejectUnsupportedAudioFormat() throws Exception {
        String base64 = Base64.getEncoder().encodeToString("fake-bytes".getBytes(StandardCharsets.UTF_8));
        String invalidMimePayload = """
                {
                  "base64Audio": "%s",
                  "mimeType": "application/pdf"
                }
                """.formatted(base64);

        mockMvc.perform(post("/api/v1/agent/voice/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidMimePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Input non valido per l'agente"));
    }

    @Test
    @DisplayName("RabbitMQ Queue + Retry: Accoda comando all'Agente AI e supera uno spike temporaneo (429 Too Many Requests) di Gemini")
    void shouldEnqueueAndRetryAgentCommandWhenGeminiExperiencesTransientSpike() throws Exception {
        String commandText = "Aggiungi 42 euro spesi da Conad per la spesa";
        String expectedReply = "Ho aggiunto 42,00 EUR da CONAD nella categoria SPESA_ALIMENTARE_E_CASA.";

        AtomicInteger attempts = new AtomicInteger(0);
        when(expenseAgentAiGateway.executeWithMcpTools(commandText)).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new RuntimeException("429 Too Many Requests: Resource has been exhausted (e.g. check quota)");
            }
            return expectedReply;
        });

        AgentJobStatusResponse queuedJob = expenseAgentQueueService.enqueueText(new AgentTextRequest(commandText));
        assertThat(queuedJob.jobId()).isNotNull();

        long deadline = System.currentTimeMillis() + 8_000;
        AgentJobStatusResponse currentStatus = queuedJob;
        while (System.currentTimeMillis() < deadline) {
            currentStatus = expenseAgentQueueService.getJobStatus(queuedJob.jobId());
            if (currentStatus.status() == AgentJobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(currentStatus.status()).isEqualTo(AgentJobStatus.COMPLETED);
        assertThat(attempts.get()).isEqualTo(2);
        assertThat(currentStatus.result()).isNotNull();
        assertThat(currentStatus.result().agentResponse()).isEqualTo(expectedReply);

        mockMvc.perform(get("/api/v1/agent/jobs/{jobId}", queuedJob.jobId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.agentResponse").value(expectedReply));
    }

    @Test
    @DisplayName("Sicurezza Bot Telegram: Blocca (401/403) secret errato o utenti Telegram estranei e accetta solo il tuo Telegram User ID")
    void shouldEnforceTelegramSecretAndUserIdWhitelist() throws Exception {
        String unauthorizedUserUpdate = """
                {
                  "update_id": 1001,
                  "message": {
                    "message_id": 50,
                    "from": { "id": 111222333, "first_name": "Estraneo", "username": "hacker" },
                    "chat": { "id": 111222333, "type": "private" },
                    "text": "Aggiungi 999 euro di spesa"
                  }
                }
                """;

        // 1. Secret Token del Webhook mancante o errato -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TelegramBotService.TELEGRAM_SECRET_HEADER, "wrong-secret")
                        .content(unauthorizedUserUpdate))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Accesso Bot Telegram negato"));

        // 2. Secret Token valido ma Telegram User ID (111222333) non nella whitelist (777888999) -> 403 Forbidden
        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TelegramBotService.TELEGRAM_SECRET_HEADER, "my-telegram-webhook-secret")
                        .content(unauthorizedUserUpdate))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Accesso Bot Telegram negato"));

        // 3. Utente autorizzato (777888999) che invia una foto scontrino su Telegram -> inoltrata a receipt-ocr-service con token M2M
        when(telegramBotGateway.downloadTelegramFile("photo-file-id-hd"))
                .thenReturn("fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));
        when(telegramBotGateway.enqueueReceiptPhotoOcr(any(), eq("image/jpeg")))
                .thenReturn("job-ocr-123");

        String authorizedPhotoUpdate = """
                {
                  "update_id": 1002,
                  "message": {
                    "message_id": 51,
                    "from": { "id": 777888999, "first_name": "Daniele", "username": "daniele" },
                    "chat": { "id": 777888999, "type": "private" },
                    "photo": [
                      { "file_id": "photo-file-id-hd", "width": 1280, "height": 960, "file_size": 102400 }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TelegramBotService.TELEGRAM_SECRET_HEADER, "my-telegram-webhook-secret")
                        .content(authorizedPhotoUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.telegramUserId").value(777888999))
                .andExpect(jsonPath("$.messageType").value("PHOTO_RECEIPT"));
    }
}
