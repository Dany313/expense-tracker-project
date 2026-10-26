package com.daniele.receiptocr.messaging;

import com.daniele.receiptocr.dto.ReceiptBase64Request;
import com.daniele.receiptocr.dto.ReceiptExtractionResponse;
import com.daniele.receiptocr.dto.ReceiptOcrJobStatus;
import com.daniele.receiptocr.dto.ReceiptOcrJobStatusResponse;
import com.daniele.receiptocr.exception.InvalidReceiptImageException;
import com.daniele.receiptocr.exception.OcrJobNotFoundException;
import com.daniele.receiptocr.exception.UnreadableReceiptException;
import com.daniele.receiptocr.service.ReceiptOcrService;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReceiptOcrQueueService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptOcrQueueService.class);

    private final RabbitTemplate rabbitTemplate;
    private final ReceiptOcrService receiptOcrService;
    private final Map<UUID, ReceiptOcrJobStatusResponse> jobStore = new ConcurrentHashMap<>();

    public ReceiptOcrQueueService(RabbitTemplate rabbitTemplate, ReceiptOcrService receiptOcrService) {
        this.rabbitTemplate = rabbitTemplate;
        this.receiptOcrService = receiptOcrService;
    }

    public ReceiptOcrJobStatusResponse enqueueMultipart(MultipartFile file, boolean autoSaveExpense) {
        if (file == null || file.isEmpty()) {
            throw new InvalidReceiptImageException("Il file immagine dello scontrino è obbligatorio e non può essere vuoto");
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            return enqueueBase64(new ReceiptBase64Request(base64, mimeType), autoSaveExpense);
        } catch (IOException e) {
            throw new InvalidReceiptImageException("Errore durante la lettura del file immagine: " + e.getMessage(), e);
        }
    }

    public ReceiptOcrJobStatusResponse enqueueBase64(ReceiptBase64Request request, boolean autoSaveExpense) {
        if (request == null || request.base64Image() == null || request.base64Image().isBlank()) {
            throw new InvalidReceiptImageException("Il contenuto Base64 dell'immagine è obbligatorio");
        }

        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        String mimeType = (request.mimeType() != null && !request.mimeType().isBlank())
                ? request.mimeType().trim()
                : "image/jpeg";

        ReceiptOcrJobStatusResponse initialStatus = new ReceiptOcrJobStatusResponse(
                jobId,
                ReceiptOcrJobStatus.QUEUED,
                autoSaveExpense,
                null,
                "Job OCR accodato su RabbitMQ (receipt.ocr.job.queue)",
                now,
                now
        );
        jobStore.put(jobId, initialStatus);

        ReceiptOcrJobMessage message = new ReceiptOcrJobMessage(
                jobId,
                request.base64Image(),
                mimeType,
                autoSaveExpense,
                now
        );

        rabbitTemplate.convertAndSend(
                ReceiptOcrRabbitMqConfig.EXPENSE_EXCHANGE,
                ReceiptOcrRabbitMqConfig.RECEIPT_OCR_ROUTING_KEY,
                message
        );

        log.info("Job OCR scontrino [jobId={}] accodato su RabbitMQ (autoSaveExpense={})", jobId, autoSaveExpense);
        return initialStatus;
    }

    public ReceiptOcrJobStatusResponse getJobStatus(UUID jobId) {
        ReceiptOcrJobStatusResponse status = jobStore.get(jobId);
        if (status == null) {
            throw new OcrJobNotFoundException(jobId);
        }
        return status;
    }

    @RabbitListener(queues = ReceiptOcrRabbitMqConfig.RECEIPT_OCR_QUEUE)
    public void processReceiptOcrJob(ReceiptOcrJobMessage job) {
        log.info("Elaborazione job OCR scontrino da coda RabbitMQ [jobId={}]", job.jobId());
        Instant createdAt = job.createdAt() != null ? job.createdAt() : Instant.now();

        jobStore.put(job.jobId(), new ReceiptOcrJobStatusResponse(
                job.jobId(),
                ReceiptOcrJobStatus.PROCESSING,
                job.autoSaveExpense(),
                null,
                "Analisi scontrino con Gemini Vision in corso...",
                createdAt,
                Instant.now()
        ));

        try {
            ReceiptExtractionResponse extracted = receiptOcrService.extractFromBase64(
                    new ReceiptBase64Request(job.base64Image(), job.mimeType())
            );

            if (job.autoSaveExpense()) {
                ExpenseCreateEventMessage createEvent = new ExpenseCreateEventMessage(
                        UUID.randomUUID(),
                        "receipt-ocr-service",
                        extracted.expenseDate(),
                        extracted.storeName(),
                        extracted.amount(),
                        extracted.category(),
                        extracted.subcategory(),
                        extracted.description(),
                        extracted.recurring(),
                        Instant.now()
                );
                rabbitTemplate.convertAndSend(
                        ReceiptOcrRabbitMqConfig.EXPENSE_EXCHANGE,
                        ReceiptOcrRabbitMqConfig.EXPENSE_CREATE_ROUTING_KEY,
                        createEvent
                );
                log.info("Spesa estratta da scontrino [jobId={}] pubblicata su coda '{}' per il salvataggio asincrono",
                        job.jobId(), ReceiptOcrRabbitMqConfig.EXPENSE_CREATE_QUEUE);
            }

            String completionMessage = job.autoSaveExpense()
                    ? "Scontrino elaborato con successo e spesa inviata alla coda expense.create.queue"
                    : "Scontrino elaborato con successo";

            jobStore.put(job.jobId(), new ReceiptOcrJobStatusResponse(
                    job.jobId(),
                    ReceiptOcrJobStatus.COMPLETED,
                    job.autoSaveExpense(),
                    extracted,
                    completionMessage,
                    createdAt,
                    Instant.now()
            ));
        } catch (InvalidReceiptImageException | UnreadableReceiptException ex) {
            log.warn("Job OCR [jobId={}] fallito per immagine non valida/illeggibile (invio diretto in DLQ): {}",
                    job.jobId(), ex.getMessage());
            jobStore.put(job.jobId(), new ReceiptOcrJobStatusResponse(
                    job.jobId(),
                    ReceiptOcrJobStatus.FAILED_DLQ,
                    job.autoSaveExpense(),
                    null,
                    ex.getMessage(),
                    createdAt,
                    Instant.now()
            ));
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.warn("Errore temporaneo Gemini API durante il job OCR [jobId={}]: {}. Attivazione retry con backoff...",
                    job.jobId(), ex.getMessage());
            jobStore.put(job.jobId(), new ReceiptOcrJobStatusResponse(
                    job.jobId(),
                    ReceiptOcrJobStatus.PROCESSING,
                    job.autoSaveExpense(),
                    null,
                    "Servizio AI temporaneamente sovraccarico, nuovo tentativo automatico in corso: " + ex.getMessage(),
                    createdAt,
                    Instant.now()
            ));
            throw ex;
        }
    }

    @RabbitListener(queues = ReceiptOcrRabbitMqConfig.RECEIPT_OCR_DLQ)
    public void handleDeadLetterReceiptOcrJob(ReceiptOcrJobMessage failedJob) {
        log.error("Job OCR scontrino [jobId={}] spostato nella Dead Letter Queue ({})",
                failedJob.jobId(), ReceiptOcrRabbitMqConfig.RECEIPT_OCR_DLQ);
        ReceiptOcrJobStatusResponse current = jobStore.get(failedJob.jobId());
        String errorMessage = (current != null && current.message() != null)
                ? current.message()
                : "Job OCR terminato in Dead Letter Queue dopo aver esaurito i tentativi disponibili";
        Instant createdAt = failedJob.createdAt() != null ? failedJob.createdAt() : Instant.now();

        jobStore.put(failedJob.jobId(), new ReceiptOcrJobStatusResponse(
                failedJob.jobId(),
                ReceiptOcrJobStatus.FAILED_DLQ,
                failedJob.autoSaveExpense(),
                null,
                errorMessage,
                createdAt,
                Instant.now()
        ));
    }
}
