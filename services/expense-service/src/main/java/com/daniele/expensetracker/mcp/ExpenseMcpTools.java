package com.daniele.expensetracker.mcp;

import com.daniele.expensetracker.domain.ExpenseCategory;
import com.daniele.expensetracker.dto.CategoryMetadataResponse;
import com.daniele.expensetracker.dto.ExpenseCreateRequest;
import com.daniele.expensetracker.dto.ExpenseFilterCriteria;
import com.daniele.expensetracker.dto.ExpenseResponse;
import com.daniele.expensetracker.dto.ExpenseSummaryResponse;
import com.daniele.expensetracker.dto.ExpenseUpdateRequest;
import com.daniele.expensetracker.service.ExpenseService;
import com.daniele.expensetracker.service.StoreNameNormalizer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMcpTools {

    private final ExpenseService expenseService;
    private final StoreNameNormalizer storeNameNormalizer;

    public ExpenseMcpTools(ExpenseService expenseService, StoreNameNormalizer storeNameNormalizer) {
        this.expenseService = expenseService;
        this.storeNameNormalizer = storeNameNormalizer;
    }

    @Tool(
            name = "create_expense",
            description = """
                    Inserisce una nuova spesa nel database PostgreSQL.
                    Permette all'agente di registrare la data (YYYY-MM-DD, default oggi se omessa),
                    il negozio, l'importo, la categoria principale, una sottocategoria opzionale,
                    una descrizione/nota opzionale e se la spesa è ricorrente.
                    """
    )
    public ExpenseResponse createExpense(
            @ToolParam(description = "Data della spesa in formato ISO YYYY-MM-DD (opzionale, se omessa usa la data odierna)", required = false)
            String expenseDate,
            @ToolParam(description = "Nome del negozio o esercente (es. CONAD, ALDI, ESSELUNGA, AMAZON)")
            String storeName,
            @ToolParam(description = "Importo numerico positivo della spesa (es. 25.50)")
            BigDecimal amount,
            @ToolParam(description = "Categoria principale della spesa: GROCERIES, DINING, TRANSPORT, HOUSING, BILLS_AND_UTILITIES, HEALTH, PERSONAL_CARE, SHOPPING, ENTERTAINMENT, TRAVEL, EDUCATION, OTHER (accetta anche termini in italiano come ALIMENTARI, RISTORAZIONE, TRASPORTI, CASA, BOLLETTE, SALUTE, CURA_PERSONA, SVAGO, VIAGGI, ISTRUZIONE, ALTRO)")
            String category,
            @ToolParam(description = "Sottocategoria opzionale (es. 'Spesa settimanale', 'Carburante', 'Farmaci', 'Abbonamento streaming')", required = false)
            String subcategory,
            @ToolParam(description = "Descrizione o note aggiuntive sulla spesa (opzionale)", required = false)
            String description,
            @ToolParam(description = "True se è una spesa fissa/ricorrente, false altrimenti (opzionale, default false)", required = false)
            Boolean recurring,
            @ToolParam(description = "True se la spesa è condivisa al 50% con qualcun altro, false altrimenti (opzionale, default false)", required = false)
            Boolean shared
    ) {
        LocalDate parsedDate = (expenseDate != null && !expenseDate.isBlank())
                ? LocalDate.parse(expenseDate.trim())
                : LocalDate.now();

        ExpenseCategory parsedCategory = ExpenseCategory.fromFlexibleString(category);

        ExpenseCreateRequest request = new ExpenseCreateRequest(
                parsedDate,
                storeName,
                amount,
                parsedCategory,
                subcategory,
                description,
                recurring != null ? recurring : false,
                shared != null ? shared : false
        );
        return expenseService.createExpense(request);
    }

    @Tool(
            name = "get_expense_by_id",
            description = "Recupera i dettagli completi di una singola spesa a partire dal suo identificativo UUID."
    )
    public ExpenseResponse getExpenseById(
            @ToolParam(description = "UUID univoco della spesa da recuperare")
            String expenseId
    ) {
        return expenseService.getExpenseById(UUID.fromString(expenseId.trim()));
    }

    @Tool(
            name = "search_expenses",
            description = """
                    Cerca e filtra le spese registrate nel database PostgreSQL.
                    Tutti i parametri di filtro sono opzionali e combinabili tra loro.
                    Restituisce le spese ordinate dalla più recente alla meno recente.
                    """
    )
    public List<ExpenseResponse> searchExpenses(
            @ToolParam(description = "Data iniziale inclusa in formato YYYY-MM-DD (opzionale)", required = false)
            String startDate,
            @ToolParam(description = "Data finale inclusa in formato YYYY-MM-DD (opzionale)", required = false)
            String endDate,
            @ToolParam(description = "Nome (o parte del nome) del negozio da cercare (opzionale)", required = false)
            String storeName,
            @ToolParam(description = "Categoria della spesa (es. GROCERIES, DINING, TRANSPORT, ecc., opzionale)", required = false)
            String category,
            @ToolParam(description = "Sottocategoria (anche parziale) da cercare (opzionale)", required = false)
            String subcategory,
            @ToolParam(description = "Importo minimo (opzionale)", required = false)
            BigDecimal minAmount,
            @ToolParam(description = "Importo massimo (opzionale)", required = false)
            BigDecimal maxAmount,
            @ToolParam(description = "Filtra solo spese ricorrenti (true) o non ricorrenti (false) (opzionale)", required = false)
            Boolean recurring,
            @ToolParam(description = "Filtra solo spese condivise (true) o personali (false) (opzionale)", required = false)
            Boolean shared,
            @ToolParam(description = "Numero massimo di risultati da restituire (default 50, max 200)", required = false)
            Integer limit
    ) {
        ExpenseFilterCriteria criteria = new ExpenseFilterCriteria(
                startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate.trim()) : null,
                endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate.trim()) : null,
                storeName,
                category != null && !category.isBlank() ? ExpenseCategory.fromFlexibleString(category) : null,
                subcategory,
                minAmount,
                maxAmount,
                recurring,
                shared
        );
        return expenseService.listExpenses(criteria, limit != null ? limit : 50);
    }

    @Tool(
            name = "update_expense",
            description = """
                    Aggiorna una spesa esistente identificata dal suo UUID.
                    Solo i campi valorizzati vengono aggiornati; i campi lasciati null rimangono invariati.
                    """
    )
    public ExpenseResponse updateExpense(
            @ToolParam(description = "UUID univoco della spesa da aggiornare")
            String expenseId,
            @ToolParam(description = "Nuova data della spesa YYYY-MM-DD (opzionale)", required = false)
            String expenseDate,
            @ToolParam(description = "Nuovo nome del negozio (opzionale)", required = false)
            String storeName,
            @ToolParam(description = "Nuovo importo positivo della spesa (opzionale)", required = false)
            BigDecimal amount,
            @ToolParam(description = "Nuova categoria della spesa (opzionale)", required = false)
            String category,
            @ToolParam(description = "Nuova sottocategoria della spesa (opzionale)", required = false)
            String subcategory,
            @ToolParam(description = "Nuova descrizione o nota (opzionale)", required = false)
            String description,
            @ToolParam(description = "Nuovo flag spesa ricorrente (opzionale)", required = false)
            Boolean recurring,
            @ToolParam(description = "Nuovo flag spesa condivisa al 50% (opzionale)", required = false)
            Boolean shared
    ) {
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                expenseDate != null && !expenseDate.isBlank() ? LocalDate.parse(expenseDate.trim()) : null,
                storeName,
                amount,
                category != null && !category.isBlank() ? ExpenseCategory.fromFlexibleString(category) : null,
                subcategory,
                description,
                recurring,
                shared
        );
        return expenseService.updateExpense(UUID.fromString(expenseId.trim()), request);
    }

    @Tool(
            name = "delete_expense",
            description = "Elimina definitivamente una spesa dal database PostgreSQL tramite il suo UUID."
    )
    public Map<String, Object> deleteExpense(
            @ToolParam(description = "UUID univoco della spesa da eliminare")
            String expenseId
    ) {
        UUID id = UUID.fromString(expenseId.trim());
        expenseService.deleteExpense(id);
        return Map.of(
                "deleted", true,
                "expenseId", id.toString(),
                "message", "Spesa eliminata con successo"
        );
    }

    @Tool(
            name = "get_expense_summary",
            description = """
                    Calcola il riepilogo delle spese (conteggio totale, importo totale, spesa media,
                    totale aggregato per categoria e per negozio) in un intervallo di date opzionale.
                    """
    )
    public ExpenseSummaryResponse getExpenseSummary(
            @ToolParam(description = "Data inizio periodo in formato YYYY-MM-DD (opzionale)", required = false)
            String startDate,
            @ToolParam(description = "Data fine periodo in formato YYYY-MM-DD (opzionale)", required = false)
            String endDate
    ) {
        LocalDate parsedStart = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate.trim()) : null;
        LocalDate parsedEnd = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate.trim()) : null;
        return expenseService.getSummary(parsedStart, parsedEnd);
    }

    @Tool(
            name = "list_expense_metadata",
            description = "Restituisce le categorie di spesa supportate e le mappature dei negozi noti."
    )
    public Map<String, Object> listExpenseMetadata() {
        List<CategoryMetadataResponse> categories = expenseService.getCategories();
        return Map.of(
                "categories", categories,
                "knownStoreMappings", storeNameNormalizer.getKnownStoreAliases()
        );
    }
}
