package com.daniele.expensetracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daniele.expensetracker.domain.ExpenseCategory;
import com.daniele.expensetracker.dto.ExpenseResponse;
import com.daniele.expensetracker.dto.ExpenseSummaryResponse;
import com.daniele.expensetracker.mcp.ExpenseMcpTools;
import com.daniele.expensetracker.messaging.ExpenseCreateEventMessage;
import com.daniele.expensetracker.messaging.ExpenseRabbitMqConfig;
import com.daniele.expensetracker.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "security.m2m.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExpenseTrackerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseMcpTools expenseMcpTools;

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
    }

    @Test
    @DisplayName("REST API: Ciclo completo CRUD, filtri e riepilogo spese su PostgreSQL")
    void shouldPerformFullRestCrudAndSummaryLifecycle() throws Exception {
        String createPayload = """
                {
                  "expenseDate": "2026-10-19",
                  "storeName": "Spazio Conad",
                  "amount": 48.90,
                  "category": "SPESA_ALIMENTARE_E_CASA",
                  "subcategory": "Spesa settimanale",
                  "description": "Spesa alimentare con prodotti freschi",
                  "recurring": false
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.storeName").value("CONAD"))
                .andExpect(jsonPath("$.amount").value(48.90))
                .andExpect(jsonPath("$.category").value("SPESA_ALIMENTARE_E_CASA"))
                .andExpect(jsonPath("$.subcategory").value("Spesa settimanale"))
                .andReturn();

        ExpenseResponse created = jsonMapper.readValue(
                createResult.getResponse().getContentAsString(),
                ExpenseResponse.class
        );

        // GET by ID
        mockMvc.perform(get("/api/v1/expenses/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.storeName").value("CONAD"));

        // PATCH partial update (e.g. changing subcategory and amount)
        String patchPayload = """
                {
                  "amount": 52.40,
                  "subcategory": "Spesa settimanale e bevande"
                }
                """;

        mockMvc.perform(patch("/api/v1/expenses/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(52.40))
                .andExpect(jsonPath("$.subcategory").value("Spesa settimanale e bevande"))
                .andExpect(jsonPath("$.storeName").value("CONAD"));

        // PUT full replacement (with optional subcategory omitted/null)
        String putPayload = """
                {
                  "expenseDate": "2026-10-18",
                  "storeName": "Zara",
                  "amount": 19.50,
                  "category": "ABBIGLIAMENTO",
                  "subcategory": null,
                  "description": "Maglietta cotone",
                  "recurring": false
                }
                """;

        mockMvc.perform(put("/api/v1/expenses/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(putPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("ZARA"))
                .andExpect(jsonPath("$.category").value("ABBIGLIAMENTO"))
                .andExpect(jsonPath("$.subcategory").isEmpty());

        // Search with filters
        mockMvc.perform(get("/api/v1/expenses")
                        .param("category", "ABBIGLIAMENTO")
                        .param("storeName", "zar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].storeName").value("ZARA"));

        // Summary endpoint
        mockMvc.perform(get("/api/v1/expenses/summary")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.totalAmount").value(19.50))
                .andExpect(jsonPath("$.amountByCategory.ABBIGLIAMENTO").value(19.50));

        // DELETE
        mockMvc.perform(delete("/api/v1/expenses/{id}", created.id()))
                .andExpect(status().isNoContent());

        // Verify 404 ProblemDetail after deletion
        mockMvc.perform(get("/api/v1/expenses/{id}", created.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Risorsa non trovata"));
    }

    @Test
    @DisplayName("REST API: Validazione Bean Validation restituisce RFC 9457 ProblemDetail (400 Bad Request)")
    void shouldReturnProblemDetailOnInvalidCreateRequest() throws Exception {
        String invalidPayload = """
                {
                  "expenseDate": null,
                  "storeName": "",
                  "amount": -10.00,
                  "category": null
                }
                """;

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Errore di validazione"))
                .andExpect(jsonPath("$.errors.expenseDate").exists())
                .andExpect(jsonPath("$.errors.storeName").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.category").exists());
    }

    @Test
    @DisplayName("MCP Server: Registrazione ToolCallbacks ed esecuzione tool CRUD per agenti AI")
    void shouldRegisterAndExecuteMcpToolsForAgent() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        List<String> toolNames = Arrays.stream(callbacks)
                .map(cb -> cb.getToolDefinition().name())
                .toList();

        assertThat(toolNames).containsExactlyInAnyOrder(
                "create_expense",
                "get_expense_by_id",
                "search_expenses",
                "update_expense",
                "delete_expense",
                "get_expense_summary",
                "list_expense_metadata"
        );

        // 1. Agent creates an expense using Italian category alias and optional subcategory
        ExpenseResponse created = expenseMcpTools.createExpense(
                "2026-10-19",
                "Aldi Suisse",
                new BigDecimal("34.20"),
                "ALIMENTARI",
                "Frutta e verdura",
                "Spesa veloce",
                false,
                false
        );

        assertThat(created.id()).isNotNull();
        assertThat(created.storeName()).isEqualTo("ALDI");
        assertThat(created.category()).isEqualTo(ExpenseCategory.SPESA_ALIMENTARE_E_CASA);
        assertThat(created.subcategory()).isEqualTo("Frutta e verdura");

        // 2. Agent searches expenses
        List<ExpenseResponse> found = expenseMcpTools.searchExpenses(
                "2026-10-01",
                "2026-10-31",
                "ALDI",
                "SPESA_ALIMENTARE_E_CASA",
                "Frutta",
                null,
                null,
                null,
                null,
                10
        );
        assertThat(found).hasSize(1);

        // 3. Agent updates the expense
        ExpenseResponse updated = expenseMcpTools.updateExpense(
                created.id().toString(),
                null,
                null,
                new BigDecimal("39.90"),
                null,
                "Frutta, verdura e latticini",
                "Aggiunto latte e formaggio",
                null,
                null
        );
        assertThat(updated.amount()).isEqualByComparingTo("39.90");
        assertThat(updated.subcategory()).isEqualTo("Frutta, verdura e latticini");

        // 4. Agent gets expense summary
        ExpenseSummaryResponse summary = expenseMcpTools.getExpenseSummary("2026-10-01", "2026-10-31");
        assertThat(summary.totalCount()).isEqualTo(1);
        assertThat(summary.totalAmount()).isEqualByComparingTo("39.90");

        // 5. Agent deletes the expense
        Map<String, Object> deleteResult = expenseMcpTools.deleteExpense(created.id().toString());
        assertThat(deleteResult).containsEntry("deleted", true);
        assertThat(expenseRepository.count()).isZero();
    }

    @Test
    @DisplayName("RabbitMQ Queue: Consuma ExpenseCreateEventMessage dalla coda expense.create.queue e salva su PostgreSQL")
    void shouldConsumeExpenseCreateCommandFromRabbitMqQueue() {
        ExpenseCreateEventMessage eventMessage = new ExpenseCreateEventMessage(
                UUID.randomUUID(),
                "receipt-ocr-service",
                LocalDate.of(2026, 10, 22),
                "Spazio Conad",
                new BigDecimal("63.45"),
                "SPESA_ALIMENTARE_E_CASA",
                "Spesa da scontrino OCR",
                "Importato via RabbitMQ da receipt-ocr-service",
                false
        );

        rabbitTemplate.convertAndSend(
                ExpenseRabbitMqConfig.EXCHANGE_NAME,
                ExpenseRabbitMqConfig.EXPENSE_CREATE_ROUTING_KEY,
                eventMessage
        );

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    var all = expenseRepository.findAll();
                    assertThat(all).hasSize(1);
                    assertThat(all.getFirst().getStoreName()).isEqualTo("CONAD");
                    assertThat(all.getFirst().getAmount()).isEqualByComparingTo("63.45");
                    assertThat(all.getFirst().getCategory()).isEqualTo(ExpenseCategory.SPESA_ALIMENTARE_E_CASA);
                    assertThat(all.getFirst().getSubcategory()).isEqualTo("Spesa da scontrino OCR");
                });
    }
}
