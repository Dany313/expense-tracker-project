package com.daniele.expenseagent.service;

import com.daniele.expenseagent.dto.AgentInteractionResponse;
import com.daniele.expenseagent.dto.AgentTextRequest;
import com.daniele.expenseagent.dto.AgentVoiceBase64Request;
import com.daniele.expenseagent.dto.InputType;
import com.daniele.expenseagent.exception.InvalidAgentInputException;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExpenseAgentOrchestratorServiceImpl implements ExpenseAgentOrchestratorService {

    private static final Set<String> SUPPORTED_AUDIO_MIME_TYPES = Set.of(
            "audio/ogg",
            "audio/oga",
            "audio/opus",
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav",
            "audio/mp4",
            "audio/m4a",
            "audio/x-m4a",
            "audio/webm",
            "audio/aac"
    );

    private final VoiceTranscriptionService voiceTranscriptionService;
    private final ExpenseAgentAiGateway expenseAgentAiGateway;

    public ExpenseAgentOrchestratorServiceImpl(
            VoiceTranscriptionService voiceTranscriptionService,
            ExpenseAgentAiGateway expenseAgentAiGateway
    ) {
        this.voiceTranscriptionService = voiceTranscriptionService;
        this.expenseAgentAiGateway = expenseAgentAiGateway;
    }

    @Override
    public AgentInteractionResponse processText(AgentTextRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new InvalidAgentInputException("Il testo del messaggio non può essere vuoto");
        }
        String cleanedText = request.text().trim();
        String agentResponse = expenseAgentAiGateway.executeWithMcpTools(cleanedText);
        return new AgentInteractionResponse(
                InputType.TEXT,
                cleanedText,
                agentResponse,
                Instant.now()
        );
    }

    @Override
    public AgentInteractionResponse processVoiceMultipart(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new InvalidAgentInputException("Il file vocale/audio è vuoto o mancante");
        }
        try {
            String contentType = audioFile.getContentType() != null ? audioFile.getContentType() : "audio/ogg";
            return processAudioBytes(audioFile.getBytes(), contentType);
        } catch (IOException e) {
            throw new InvalidAgentInputException("Errore nella lettura del file audio: " + e.getMessage(), e);
        }
    }

    @Override
    public AgentInteractionResponse processVoiceBase64(AgentVoiceBase64Request request) {
        if (request == null || request.base64Audio() == null || request.base64Audio().isBlank()) {
            throw new InvalidAgentInputException("Il contenuto Base64 dell'audio è vuoto o mancante");
        }
        String rawBase64 = request.base64Audio().trim();
        String detectedMime = request.mimeType();

        if (rawBase64.startsWith("data:") && rawBase64.contains(",")) {
            int commaIndex = rawBase64.indexOf(',');
            String header = rawBase64.substring(5, commaIndex);
            if (header.contains(";")) {
                detectedMime = header.substring(0, header.indexOf(';'));
            }
            rawBase64 = rawBase64.substring(commaIndex + 1);
        }

        try {
            byte[] audioBytes = Base64.getMimeDecoder().decode(rawBase64);
            if (audioBytes.length == 0) {
                throw new InvalidAgentInputException("L'audio decodificato da Base64 è vuoto");
            }
            return processAudioBytes(audioBytes, detectedMime != null ? detectedMime : "audio/ogg");
        } catch (IllegalArgumentException e) {
            throw new InvalidAgentInputException("Stringa Base64 audio non valida: " + e.getMessage(), e);
        }
    }

    private AgentInteractionResponse processAudioBytes(byte[] audioBytes, String contentType) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new InvalidAgentInputException("I byte del messaggio vocale sono vuoti");
        }

        String normalizedMime = normalizeAudioMimeType(contentType);
        if (!SUPPORTED_AUDIO_MIME_TYPES.contains(normalizedMime)) {
            throw new InvalidAgentInputException(
                    "Formato audio non supportato: '" + contentType + "'. Formati ammessi: " + SUPPORTED_AUDIO_MIME_TYPES
            );
        }

        MimeType mimeType = MimeTypeUtils.parseMimeType(canonicalizeAudioMime(normalizedMime));

        // 1. Traduce/trascrive il vocale in testo
        String transcribedText = voiceTranscriptionService.transcribeAudioToText(audioBytes, mimeType);
        if (transcribedText == null || transcribedText.isBlank()) {
            throw new InvalidAgentInputException("Impossibile trascrivere alcun testo dal messaggio vocale fornito");
        }

        // 2. Invia il testo trascritto a Gemini connesso all'MCP del microservizio principale
        String agentResponse = expenseAgentAiGateway.executeWithMcpTools(transcribedText.trim());

        return new AgentInteractionResponse(
                InputType.VOICE,
                transcribedText.trim(),
                agentResponse,
                Instant.now()
        );
    }

    private String normalizeAudioMimeType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "audio/ogg";
        }
        String lower = contentType.trim().toLowerCase(Locale.ROOT);
        if (lower.contains(";")) {
            lower = lower.substring(0, lower.indexOf(';')).trim();
        }
        return lower;
    }

    private String canonicalizeAudioMime(String normalizedMime) {
        return switch (normalizedMime) {
            case "audio/oga", "audio/opus" -> "audio/ogg";
            case "audio/mp3" -> "audio/mpeg";
            case "audio/x-wav" -> "audio/wav";
            case "audio/m4a", "audio/x-m4a" -> "audio/mp4";
            default -> normalizedMime;
        };
    }
}
