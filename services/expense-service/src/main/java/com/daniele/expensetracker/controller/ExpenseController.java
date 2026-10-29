package com.daniele.expensetracker.controller;

import com.daniele.expensetracker.dto.CategoryMetadataResponse;
import com.daniele.expensetracker.dto.CategoryRequest;
import com.daniele.expensetracker.dto.ExpenseCreateRequest;
import com.daniele.expensetracker.dto.ExpenseFilterCriteria;
import com.daniele.expensetracker.dto.ExpenseResponse;
import com.daniele.expensetracker.dto.ExpenseSummaryResponse;
import com.daniele.expensetracker.dto.ExpenseUpdateRequest;
import com.daniele.expensetracker.service.CategoryService;
import com.daniele.expensetracker.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
@Tag(name = "Expenses", description = "API REST per la gestione delle spese e delle categorie memorizzate nel database")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;

    public ExpenseController(ExpenseService expenseService, CategoryService categoryService) {
        this.expenseService = expenseService;
        this.categoryService = categoryService;
    }

    @PostMapping
    @Operation(summary = "Crea una nuova spesa", description = "Inserisce una spesa con data, negozio, importo, categoria e metadati")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseCreateRequest request) {
        ExpenseResponse response = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Cerca e filtra le spese (paginato)", description = "Restituisce una pagina di spese filtrabili per data, negozio, categoria, sottocategoria e importo")
    public ResponseEntity<Page<ExpenseResponse>> searchExpenses(
            @Parameter(description = "Data inizio (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Data fine (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filtro parziale sul nome del negozio")
            @RequestParam(required = false) String storeName,
            @Parameter(description = "Categoria o codice categoria della spesa")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filtro parziale sulla sottocategoria")
            @RequestParam(required = false) String subcategory,
            @Parameter(description = "Importo minimo")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Importo massimo")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Filtra per spesa ricorrente")
            @RequestParam(required = false) Boolean recurring,
            @Parameter(description = "Filtra per spesa condivisa al 50%")
            @RequestParam(required = false) Boolean shared,
            @PageableDefault(size = 20, sort = {"expenseDate", "createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ExpenseFilterCriteria criteria = new ExpenseFilterCriteria(
                startDate,
                endDate,
                storeName,
                category,
                subcategory,
                minAmount,
                maxAmount,
                recurring,
                shared
        );
        return ResponseEntity.ok(expenseService.searchExpenses(criteria, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ottieni una spesa per ID", description = "Recupera i dettagli di una singola spesa tramite il suo UUID")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aggiorna completamente una spesa (PUT)", description = "Sostituisce tutti i dati di una spesa esistente")
    public ResponseEntity<ExpenseResponse> replaceExpense(
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseCreateRequest request
    ) {
        return ResponseEntity.ok(expenseService.replaceExpense(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Aggiorna parzialmente una spesa (PATCH)", description = "Aggiorna solo i campi specificati di una spesa esistente")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseUpdateRequest request
    ) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una spesa", description = "Rimuove definitivamente una spesa tramite il suo UUID")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @Operation(summary = "Riepilogo statistico delle spese", description = "Calcola totale, media e aggregazioni per categoria e negozio nell'intervallo di date")
    public ResponseEntity<ExpenseSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") Boolean splitShared
    ) {
        return ResponseEntity.ok(expenseService.getSummary(startDate, endDate, Boolean.TRUE.equals(splitShared)));
    }

    @GetMapping("/categories")
    @Operation(summary = "Elenco categorie disponibili", description = "Restituisce tutte le categorie di spesa registrate nel database")
    public ResponseEntity<List<CategoryMetadataResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping("/categories")
    @Operation(summary = "Crea una nuova categoria", description = "Aggiunge una nuova categoria di spesa con codice, nome e sinonimi nel database")
    public ResponseEntity<CategoryMetadataResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryMetadataResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/categories/{code}")
    @Operation(summary = "Aggiorna una categoria", description = "Modifica il nome visualizzato o i sinonimi di una categoria esistente a database")
    public ResponseEntity<CategoryMetadataResponse> updateCategory(
            @PathVariable String code,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(code, request));
    }

    @DeleteMapping("/categories/{code}")
    @Operation(summary = "Elimina una categoria", description = "Rimuove una categoria dal database (se non referenziata da spese)")
    public ResponseEntity<Void> deleteCategory(@PathVariable String code) {
        categoryService.deleteCategory(code);
        return ResponseEntity.noContent().build();
    }
}
