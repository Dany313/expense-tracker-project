package com.daniele.expenseagent.service;

import com.daniele.expenseagent.dto.AgentInteractionResponse;
import com.daniele.expenseagent.dto.AgentTextRequest;
import com.daniele.expenseagent.dto.AgentVoiceBase64Request;
import org.springframework.web.multipart.MultipartFile;

public interface ExpenseAgentOrchestratorService {

    AgentInteractionResponse processText(AgentTextRequest request);

    AgentInteractionResponse processVoiceMultipart(MultipartFile audioFile);

    AgentInteractionResponse processVoiceBase64(AgentVoiceBase64Request request);
}
