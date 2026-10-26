package com.daniele.expenseagent.messaging;

import com.daniele.expenseagent.dto.InputType;
import java.time.Instant;
import java.util.UUID;

public record AgentJobMessage(
        UUID jobId,
        InputType inputType,
        String text,
        String base64Audio,
        String mimeType,
        Instant createdAt
) {
}
