package com.daniele.expenseagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Richiesta JSON con messaggio vocale/audio codificato in Base64")
public record AgentVoiceBase64Request(

        @NotBlank(message = "Il contenuto audio in Base64 è obbligatorio")
        @Schema(description = "Audio vocale codificato in Base64 (es. vocale Telegram .ogg, .mp3, .wav, .m4a)")
        String base64Audio,

        @Schema(description = "MIME type dell'audio (es. audio/ogg, audio/mpeg, audio/wav, audio/mp4). Default: audio/ogg", example = "audio/ogg", defaultValue = "audio/ogg")
        String mimeType
) {
}
