package com.daniele.expenseagent.service;

import org.springframework.util.MimeType;

public interface VoiceTranscriptionService {

    String transcribeAudioToText(byte[] audioBytes, MimeType mimeType);
}
