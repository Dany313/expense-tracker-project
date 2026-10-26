package com.daniele.expenseagent.messaging;

import com.daniele.expenseagent.dto.AgentInteractionResponse;
import com.daniele.expenseagent.dto.AgentJobStatus;
import com.daniele.expenseagent.dto.AgentJobStatusResponse;
import com.daniele.expenseagent.dto.AgentTextRequest;
import com.daniele.expenseagent.dto.AgentVoiceBase64Request;
import com.daniele.expenseagent.dto.InputType;
import com.daniele.expenseagent.exception.AgentJobNotFoundException;
import com.daniele.expenseagent.exception.InvalidAgentInputException;
import com.daniele.expenseagent.service.ExpenseAgentOrchestratorService;
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
public class ExpenseAgentQueueService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseAgentQueueService.class);

    private final RabbitTemplate rabbitTemplate;
    private final ExpenseAgentOrchestratorService orchestratorService;
    private final Map<UUID, AgentJobStatusResponse> jobStore = new ConcurrentHashMap<>();

    public ExpenseAgentQueueService(
            RabbitTemplate rabbitTemplate,
            ExpenseAgentOrchestratorService orchestratorService
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.orchestratorService = orchestratorService;
    }

    public AgentJobStatusResponse enqueueText(AgentTextRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new InvalidAgentInputException("Il testo del messaggio non può essere vuoto");
        }

        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();

        AgentJobStatusResponse initialStatus = new AgentJobStatusResponse(
                jobId,
                InputType.TEXT,
                AgentJobStatus.QUEUED,
                null,
                "Comando testuale accodato su RabbitMQ (agent.command.job.queue)",
                now,
                now
        );
        jobStore.put(jobId, initialStatus);

        AgentJobMessage message = new AgentJobMessage(
                jobId,
                InputType.TEXT,
                request.text().trim(),
                null,
                null,
                now
        );

        rabbitTemplate.convertAndSend(
                ExpenseAgentRabbitMqConfig.EXPENSE_EXCHANGE,
                ExpenseAgentRabbitMqConfig.AGENT_COMMAND_ROUTING_KEY,
                message
        );

        log.info("Job testuale Agente AI [jobId={}] accodato su RabbitMQ", jobId);
        return initialStatus;
    }

    public AgentJobStatusResponse enqueueVoiceMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAgentInputException("Il file audio del messaggio vocale è obbligatorio e non può essere vuoto");
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "audio/ogg";
            return enqueueVoiceBase64(new AgentVoiceBase64Request(base64, mimeType));
        } catch (IOException e) {
            throw new InvalidAgentInputException("Impossibile leggere il file audio: " + e.getMessage(), e);
        }
    }

    public AgentJobStatusResponse enqueueVoiceBase64(AgentVoiceBase64Request request) {
        if (request == null || request.base64Audio() == null || request.base64Audio().isBlank()) {
            throw new InvalidAgentInputException("Il contenuto audio in Base64 è obbligatorio");
        }

        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        String mimeType = (request.mimeType() != null && !request.mimeType().isBlank())
                ? request.mimeType().trim()
                : "audio/ogg";

        AgentJobStatusResponse initialStatus = new AgentJobStatusResponse(
                jobId,
                InputType.VOICE,
                AgentJobStatus.QUEUED,
                null,
                "Messaggio vocale accodato su RabbitMQ (agent.command.job.queue)",
                now,
                now
        );
        jobStore.put(jobId, initialStatus);

        AgentJobMessage message = new AgentJobMessage(
                jobId,
                InputType.VOICE,
                null,
                request.base64Audio(),
                mimeType,
                now
        );

        rabbitTemplate.convertAndSend(
                ExpenseAgentRabbitMqConfig.EXPENSE_EXCHANGE,
                ExpenseAgentRabbitMqConfig.AGENT_COMMAND_ROUTING_KEY,
                message
        );

        log.info("Job vocale Agente AI [jobId={}] accodato su RabbitMQ", jobId);
        return initialStatus;
    }

    public AgentJobStatusResponse getJobStatus(UUID jobId) {
        AgentJobStatusResponse status = jobStore.get(jobId);
        if (status == null) {
            throw new AgentJobNotFoundException(jobId);
        }
        return status;
    }

    @RabbitListener(queues = ExpenseAgentRabbitMqConfig.AGENT_COMMAND_QUEUE)
    public void processAgentJob(AgentJobMessage job) {
        log.info("Elaborazione job Agente AI da coda RabbitMQ [jobId={}, inputType={}]", job.jobId(), job.inputType());
        Instant createdAt = job.createdAt() != null ? job.createdAt() : Instant.now();

        jobStore.put(job.jobId(), new AgentJobStatusResponse(
                job.jobId(),
                job.inputType(),
                AgentJobStatus.PROCESSING,
                null,
                "Elaborazione comando con Gemini e tool MCP in corso...",
                createdAt,
                Instant.now()
        ));

        try {
            AgentInteractionResponse result = switch (job.inputType()) {
                case TEXT -> orchestratorService.processText(new AgentTextRequest(job.text()));
                case VOICE -> orchestratorService.processVoiceBase64(
                        new AgentVoiceBase64Request(job.base64Audio(), job.mimeType())
                );
            };

            jobStore.put(job.jobId(), new AgentJobStatusResponse(
                    job.jobId(),
                    job.inputType(),
                    AgentJobStatus.COMPLETED,
                    result,
                    "Comando elaborato con successo da Gemini connesso all'MCP",
                    createdAt,
                    Instant.now()
            ));
        } catch (InvalidAgentInputException ex) {
            log.warn("Job Agente AI [jobId={}] fallito per input non valido (invio diretto in DLQ): {}",
                    job.jobId(), ex.getMessage());
            jobStore.put(job.jobId(), new AgentJobStatusResponse(
                    job.jobId(),
                    job.inputType(),
                    AgentJobStatus.FAILED_DLQ,
                    null,
                    ex.getMessage(),
                    createdAt,
                    Instant.now()
            ));
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.warn("Errore temporaneo Gemini API / MCP durante il job Agente AI [jobId={}]: {}. Attivazione retry con backoff...",
                    job.jobId(), ex.getMessage());
            jobStore.put(job.jobId(), new AgentJobStatusResponse(
                    job.jobId(),
                    job.inputType(),
                    AgentJobStatus.PROCESSING,
                    null,
                    "Servizio AI o MCP temporaneamente non disponibile, nuovo tentativo automatico in corso: " + ex.getMessage(),
                    createdAt,
                    Instant.now()
            ));
            throw ex;
        }
    }

    @RabbitListener(queues = ExpenseAgentRabbitMqConfig.AGENT_COMMAND_DLQ)
    public void handleDeadLetterAgentJob(AgentJobMessage failedJob) {
        log.error("Job Agente AI [jobId={}] spostato nella Dead Letter Queue ({})",
                failedJob.jobId(), ExpenseAgentRabbitMqConfig.AGENT_COMMAND_DLQ);
        AgentJobStatusResponse current = jobStore.get(failedJob.jobId());
        String errorMessage = (current != null && current.message() != null)
                ? current.message()
                : "Job Agente AI terminato in Dead Letter Queue dopo aver esaurito i tentativi disponibili";
        Instant createdAt = failedJob.createdAt() != null ? failedJob.createdAt() : Instant.now();

        jobStore.put(failedJob.jobId(), new AgentJobStatusResponse(
                failedJob.jobId(),
                failedJob.inputType(),
                AgentJobStatus.FAILED_DLQ,
                null,
                errorMessage,
                createdAt,
                Instant.now()
        ));
    }
}
