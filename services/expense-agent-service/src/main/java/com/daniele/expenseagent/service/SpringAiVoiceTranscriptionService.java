package com.daniele.expenseagent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

@Component
public class SpringAiVoiceTranscriptionService implements VoiceTranscriptionService {

    private static final String TRANSCRIPTION_SYSTEM_PROMPT = """
            Sei un trascrittore vocale ad alta precisione per un assistente di gestione spese.
            Ascolta il messaggio vocale fornito e restituisci ESCLUSIVAMENTE la trascrizione testuale fedele in italiano,
            formattando correttamente gli importi numerici (es. 32,50 euro), le date e i nomi dei negozi (es. Conad, Aldi, Tigotà, Esselunga).
            Non aggiungere commenti, saluti o testo extra oltre a quanto detto dall'utente nel vocale.
            """;

    private final ChatClient transcriptionChatClient;

    public SpringAiVoiceTranscriptionService(ChatClient.Builder chatClientBuilder) {
        this.transcriptionChatClient = chatClientBuilder
                .defaultSystem(TRANSCRIPTION_SYSTEM_PROMPT)
                .build();
    }

    @Override
    public String transcribeAudioToText(byte[] audioBytes, MimeType mimeType) {
        ByteArrayResource audioResource = new ByteArrayResource(audioBytes);
        String transcription = transcriptionChatClient.prompt()
                .user(userSpec -> userSpec
                        .text("Trascrivi fedelmente questo messaggio vocale in testo.")
                        .media(mimeType, audioResource))
                .call()
                .content();
        return transcription != null ? transcription.trim() : "";
    }
}
