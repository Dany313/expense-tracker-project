package com.daniele.expenseagent.controller;

import com.daniele.expenseagent.dto.AgentInteractionResponse;
import com.daniele.expenseagent.dto.AgentJobStatusResponse;
import com.daniele.expenseagent.dto.AgentTextRequest;
import com.daniele.expenseagent.dto.AgentVoiceBase64Request;
import com.daniele.expenseagent.messaging.ExpenseAgentQueueService;
import com.daniele.expenseagent.service.ExpenseAgentOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "Expense AI Agent", description = "Microservizio Agente AI: riceve testo o messaggi vocali, trascrive i vocali e invia la richiesta a Gemini connesso all'MCP di expense-service")
public class ExpenseAgentController {

    private final ExpenseAgentOrchestratorService orchestratorService;
    private final ExpenseAgentQueueService expenseAgentQueueService;

    public ExpenseAgentController(
            ExpenseAgentOrchestratorService orchestratorService,
            ExpenseAgentQueueService expenseAgentQueueService
    ) {
        this.orchestratorService = orchestratorService;
        this.expenseAgentQueueService = expenseAgentQueueService;
    }

    @PostMapping(path = "/text", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Invia un messaggio di testo all'Agente AI",
            description = "Inoltra il testo a Gemini, che esegue le operazioni necessarie tramite i tool MCP del microservizio principale expense-service e restituisce la risposta."
    )
    public ResponseEntity<AgentInteractionResponse> processText(@Valid @RequestBody AgentTextRequest request) {
        return ResponseEntity.ok(orchestratorService.processText(request));
    }

    @PostMapping(path = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Invia un messaggio vocale (file audio Multipart) all'Agente AI",
            description = "Traduce/trascrive il messaggio vocale (es. .ogg da Telegram, .mp3, .wav, .m4a) in testo e lo invia a Gemini connesso all'MCP del microservizio principale."
    )
    public ResponseEntity<AgentInteractionResponse> processVoiceMultipart(
            @Parameter(description = "File audio del messaggio vocale (OGG, MP3, WAV, M4A, WEBM, AAC)")
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(orchestratorService.processVoiceMultipart(file));
    }

    @PostMapping(path = "/voice/base64", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Invia un messaggio vocale in Base64 all'Agente AI",
            description = "Traduce/trascrive il messaggio vocale codificato in Base64 in testo e lo invia a Gemini connesso all'MCP del microservizio principale."
    )
    public ResponseEntity<AgentInteractionResponse> processVoiceBase64(
            @Valid @RequestBody AgentVoiceBase64Request request
    ) {
        return ResponseEntity.ok(orchestratorService.processVoiceBase64(request));
    }

    @PostMapping(path = "/text/enqueue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Accoda un comando testuale su RabbitMQ per l'Agente AI",
            description = "Accoda il messaggio di testo sulla coda agent.command.job.queue con retry esponenziale automatico e Dead Letter Queue in caso di picchi o indisponibilità temporanea di Gemini/MCP."
    )
    public ResponseEntity<AgentJobStatusResponse> enqueueText(@Valid @RequestBody AgentTextRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(expenseAgentQueueService.enqueueText(request));
    }

    @PostMapping(path = "/voice/enqueue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Accoda un messaggio vocale (Multipart) su RabbitMQ per l'Agente AI",
            description = "Accoda il file audio sulla coda agent.command.job.queue con retry esponenziale automatico e Dead Letter Queue."
    )
    public ResponseEntity<AgentJobStatusResponse> enqueueVoiceMultipart(
            @Parameter(description = "File audio del messaggio vocale")
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(expenseAgentQueueService.enqueueVoiceMultipart(file));
    }

    @PostMapping(path = "/voice/base64/enqueue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Accoda un messaggio vocale in Base64 su RabbitMQ per l'Agente AI",
            description = "Accoda il vocale codificato in Base64 sulla coda agent.command.job.queue con retry esponenziale automatico e Dead Letter Queue."
    )
    public ResponseEntity<AgentJobStatusResponse> enqueueVoiceBase64(
            @Valid @RequestBody AgentVoiceBase64Request request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(expenseAgentQueueService.enqueueVoiceBase64(request));
    }

    @GetMapping(path = "/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Controlla lo stato di un job dell'Agente AI accodato su RabbitMQ",
            description = "Restituisce lo stato corrente (QUEUED, PROCESSING, COMPLETED, FAILED_DLQ) e la risposta dell'Agente AI una volta completato."
    )
    public ResponseEntity<AgentJobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(expenseAgentQueueService.getJobStatus(jobId));
    }
}
