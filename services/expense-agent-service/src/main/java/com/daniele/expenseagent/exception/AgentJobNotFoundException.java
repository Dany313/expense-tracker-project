package com.daniele.expenseagent.exception;

import java.util.UUID;

public class AgentJobNotFoundException extends RuntimeException {

    public AgentJobNotFoundException(UUID jobId) {
        super("Job dell'Agente AI non trovato con id: " + jobId);
    }
}
