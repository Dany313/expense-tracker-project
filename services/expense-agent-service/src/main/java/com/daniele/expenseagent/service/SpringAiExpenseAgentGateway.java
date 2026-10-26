package com.daniele.expenseagent.service;

import io.modelcontextprotocol.client.McpSyncClient;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class SpringAiExpenseAgentGateway implements ExpenseAgentAiGateway {

    private static final String AGENT_SYSTEM_PROMPT = """
            Sei l'Agente AI di gestione spese personali (Expense Tracker Agent).
            Sei connesso tramite protocollo MCP (Model Context Protocol) al microservizio principale delle spese (expense-service).
            La data odierna è: {current_date}.

            COMPITI:
            1. Quando l'utente comunica una spesa (es. da testo libero, messaggio vocale trascritto o notifica bancaria/wallet),
               invoca il tool MCP `create_expense` passando data, negozio, importo, categoria appropriata,
               eventuale sottocategoria e descrizione.
            2. Quando l'utente chiede informazioni, ricerche, modifiche, cancellazioni o riepiloghi sulle proprie spese,
               invoca i corrispondenti tool MCP (`search_expenses`, `get_expense_summary`, `update_expense`, `delete_expense`, `get_expense_by_id`).
            3. Rispondi sempre in italiano in modo chiaro, conciso e amichevole, confermando i dettagli dell'operazione effettuata
               (negozio, importo, data, categoria e ID della spesa quando applicabile).
            """;

    private final ChatClient agentChatClient;
    private final ToolCallbackProvider mcpToolCallbackProvider;
    private final List<McpSyncClient> mcpSyncClients;

    public SpringAiExpenseAgentGateway(
            ChatClient.Builder chatClientBuilder,
            @Nullable ToolCallbackProvider mcpToolCallbackProvider,
            @Nullable List<McpSyncClient> mcpSyncClients
    ) {
        this.agentChatClient = chatClientBuilder.build();
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.mcpSyncClients = mcpSyncClients != null ? mcpSyncClients : List.of();
    }

    @Override
    public String executeWithMcpTools(String userText) {
        ensureMcpClientsInitialized();

        var promptSpec = agentChatClient.prompt()
                .system(s -> s.text(AGENT_SYSTEM_PROMPT)
                        .param("current_date", LocalDate.now().toString()))
                .user(userText);

        if (mcpToolCallbackProvider != null) {
            promptSpec = promptSpec.toolCallbacks(mcpToolCallbackProvider.getToolCallbacks());
        }

        String response = promptSpec.call().content();
        return response != null ? response.trim() : "";
    }

    private void ensureMcpClientsInitialized() {
        for (McpSyncClient client : mcpSyncClients) {
            if (!client.isInitialized()) {
                client.initialize();
            }
        }
    }
}
