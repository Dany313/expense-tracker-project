package com.daniele.receiptocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daniele.receiptocr.dto.ReceiptBase64Request;
import com.daniele.receiptocr.dto.ReceiptExtractionResponse;
import com.daniele.receiptocr.dto.ReceiptOcrJobStatus;
import com.daniele.receiptocr.dto.ReceiptOcrJobStatusResponse;
import com.daniele.receiptocr.mcp.ReceiptOcrMcpTools;
import com.daniele.receiptocr.messaging.ExpenseCreateEventMessage;
import com.daniele.receiptocr.messaging.ReceiptOcrQueueService;
import com.daniele.receiptocr.messaging.ReceiptOcrRabbitMqConfig;
import com.daniele.receiptocr.service.ReceiptVisionExtractor;
import com.daniele.receiptocr.service.ReceiptVisionExtractor.RawVisionReceiptResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "security.m2m.enabled=false",
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
        "spring.rabbitmq.listener.simple.retry.multiplier=1.5",
        "spring.rabbitmq.listener.simple.retry.max-attempts=3"
})
class ReceiptOcrIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReceiptOcrMcpTools receiptOcrMcpTools;

    @Autowired
    private ReceiptOcrQueueService receiptOcrQueueService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @MockitoBean
    private ReceiptVisionExtractor visionExtractor;

    @Test
    @DisplayName("REST Multipart: Estrae data, negozio normalizzato e dati spesa dalla foto dello scontrino")
    void shouldExtractExpenseDataFromMultipartReceiptImage() throws Exception {
        when(visionExtractor.extractFromImage(any(), any())).thenReturn(
                new RawVisionReceiptResult(
                        true,
                        "2026-10-19",
                        "Spazio Conad Superstore",
                        new BigDecimal("45.60"),
                        "GROCERIES",
                        "Spesa alimentare",
                        "Scontrino supermercato Conad",
                        null
                )
        );

        MockMultipartFile receiptImage = new MockMultipartFile(
                "file",
                "scontrino.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-jpeg-binary-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/ocr/receipts").file(receiptImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseDate").value("2026-10-19"))
                .andExpect(jsonPath("$.storeName").value("CONAD"))
                .andExpect(jsonPath("$.amount").value(45.60))
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.subcategory").value("Spesa alimentare"))
                .andExpect(jsonPath("$.recurring").value(false));
    }

    @Test
    @DisplayName("REST Base64 & Errori: Supporta data URI Base64 e restituisce 422 ProblemDetail se l'immagine non è uno scontrino")
    void shouldHandleBase64AndUnreadableReceipt() throws Exception {
        when(visionExtractor.extractFromImage(any(), any())).thenReturn(
                new RawVisionReceiptResult(
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "L'immagine fornita non mostra uno scontrino fiscale"
                )
        );

        String base64 = Base64.getEncoder().encodeToString("not-a-receipt".getBytes(StandardCharsets.UTF_8));
        String payload = """
                {
                  "base64Image": "data:image/png;base64,%s",
                  "mimeType": "image/png"
                }
                """.formatted(base64);

        mockMvc.perform(post("/api/v1/ocr/receipts/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Impossibile estrarre i dati dallo scontrino"))
                .andExpect(jsonPath("$.detail").value("L'immagine fornita non mostra uno scontrino fiscale"));
    }

    @Test
    @DisplayName("MCP Server: Registra ed esegue i tool MCP di OCR scontrino")
    void shouldRegisterAndExecuteMcpOcrTools() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        List<String> toolNames = Arrays.stream(callbacks)
                .map(cb -> cb.getToolDefinition().name())
                .toList();

        assertThat(toolNames).containsExactlyInAnyOrder(
                "extract_expense_from_receipt_base64",
                "extract_expense_from_receipt_file_path",
                "enqueue_receipt_ocr_base64"
        );

        when(visionExtractor.extractFromImage(any(), any())).thenReturn(
                new RawVisionReceiptResult(
                        true,
                        "2026-10-18",
                        "Tigotà Gottardo",
                        new BigDecimal("21.90"),
                        "PERSONAL_CARE",
                        "Igiene casa",
                        "Scontrino Tigotà",
                        null
                )
        );

        String base64 = Base64.getEncoder().encodeToString("fake-image".getBytes(StandardCharsets.UTF_8));
        ReceiptExtractionResponse extracted = receiptOcrMcpTools.extractExpenseFromReceiptBase64(base64, "image/jpeg");

        assertThat(extracted.expenseDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(extracted.storeName()).isEqualTo("TIGOTA");
        assertThat(extracted.amount()).isEqualByComparingTo("21.90");
        assertThat(extracted.category()).isEqualTo("PERSONAL_CARE");
        assertThat(extracted.subcategory()).isEqualTo("Igiene casa");
    }

    @Test
    @DisplayName("RabbitMQ Queue + Retry + Forwarding: Supera un errore transitorio di Gemini (503) grazie al retry e pubblica la spesa su expense.create.queue")
    void shouldRetryOnTransientGeminiSpikeAndForwardExpenseToCreateQueue() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        when(visionExtractor.extractFromImage(any(), any())).thenAnswer(invocation -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                throw new RuntimeException("503 Service Unavailable: Gemini model is currently overloaded");
            }
            return new RawVisionReceiptResult(
                    true,
                    "2026-10-22",
                    "Esselunga S.p.A.",
                    new BigDecimal("63.40"),
                    "GROCERIES",
                    "Spesa settimanale",
                    "Scontrino Esselunga tramite coda RabbitMQ",
                    null
            );
        });

        String base64 = Base64.getEncoder().encodeToString("receipt-queue-test".getBytes(StandardCharsets.UTF_8));
        ReceiptOcrJobStatusResponse queuedJob = receiptOcrQueueService.enqueueBase64(
                new ReceiptBase64Request(base64, "image/jpeg"),
                true
        );

        assertThat(queuedJob.jobId()).isNotNull();

        long deadline = System.currentTimeMillis() + 8_000;
        ReceiptOcrJobStatusResponse currentStatus = queuedJob;
        while (System.currentTimeMillis() < deadline) {
            currentStatus = receiptOcrQueueService.getJobStatus(queuedJob.jobId());
            if (currentStatus.status() == ReceiptOcrJobStatus.COMPLETED) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(currentStatus.status()).isEqualTo(ReceiptOcrJobStatus.COMPLETED);
        assertThat(attempts.get()).isEqualTo(2);
        assertThat(currentStatus.extractedExpense()).isNotNull();
        assertThat(currentStatus.extractedExpense().storeName()).isEqualTo("ESSELUNGA");
        assertThat(currentStatus.extractedExpense().amount()).isEqualByComparingTo("63.40");

        mockMvc.perform(get("/api/v1/ocr/receipts/jobs/{jobId}", queuedJob.jobId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.extractedExpense.storeName").value("ESSELUNGA"));

        ExpenseCreateEventMessage forwardedEvent = rabbitTemplate.receiveAndConvert(
                ReceiptOcrRabbitMqConfig.EXPENSE_CREATE_QUEUE,
                2_000,
                new ParameterizedTypeReference<ExpenseCreateEventMessage>() {}
        );
        assertThat(forwardedEvent).isNotNull();
        assertThat(forwardedEvent.storeName()).isEqualTo("ESSELUNGA");
        assertThat(forwardedEvent.amount()).isEqualByComparingTo("63.40");
        assertThat(forwardedEvent.sourceService()).isEqualTo("receipt-ocr-service");
    }
}
