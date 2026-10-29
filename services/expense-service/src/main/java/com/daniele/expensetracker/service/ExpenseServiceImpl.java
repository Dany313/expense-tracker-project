package com.daniele.expensetracker.service;

import com.daniele.expensetracker.domain.Expense;
import com.daniele.expensetracker.dto.CategoryMetadataResponse;
import com.daniele.expensetracker.dto.ExpenseCreateRequest;
import com.daniele.expensetracker.dto.ExpenseFilterCriteria;
import com.daniele.expensetracker.dto.ExpenseResponse;
import com.daniele.expensetracker.dto.ExpenseSummaryResponse;
import com.daniele.expensetracker.dto.ExpenseUpdateRequest;
import com.daniele.expensetracker.exception.ResourceNotFoundException;
import com.daniele.expensetracker.repository.ExpenseRepository;
import com.daniele.expensetracker.repository.ExpenseSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private static final int MAX_LIST_LIMIT = 200;

    private final ExpenseRepository expenseRepository;
    private final StoreNameNormalizer storeNameNormalizer;
    private final CategoryService categoryService;

    public ExpenseServiceImpl(
            ExpenseRepository expenseRepository,
            StoreNameNormalizer storeNameNormalizer,
            CategoryService categoryService
    ) {
        this.expenseRepository = expenseRepository;
        this.storeNameNormalizer = storeNameNormalizer;
        this.categoryService = categoryService;
    }

    @Override
    @Transactional
    public ExpenseResponse createExpense(ExpenseCreateRequest request) {
        Expense expense = new Expense();
        applyCreateRequest(expense, request);
        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Override
    public ExpenseResponse getExpenseById(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forExpenseId(id));
        return toResponse(expense);
    }

    @Override
    public Page<ExpenseResponse> searchExpenses(ExpenseFilterCriteria criteria, Pageable pageable) {
        validateDateAndAmountRange(criteria);
        Pageable resolvedPageable = pageable != null ? pageable : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "expenseDate"));
        return expenseRepository.findAll(ExpenseSpecifications.withCriteria(criteria), resolvedPageable)
                .map(this::toResponse);
    }

    @Override
    public List<ExpenseResponse> listExpenses(ExpenseFilterCriteria criteria, int limit) {
        validateDateAndAmountRange(criteria);
        int boundedLimit = Math.clamp(limit, 1, MAX_LIST_LIMIT);
        Pageable pageable = PageRequest.of(0, boundedLimit, Sort.by(Sort.Direction.DESC, "expenseDate", "createdAt"));
        return expenseRepository.findAll(ExpenseSpecifications.withCriteria(criteria), pageable)
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ExpenseResponse replaceExpense(UUID id, ExpenseCreateRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forExpenseId(id));
        applyCreateRequest(expense, request);
        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(UUID id, ExpenseUpdateRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forExpenseId(id));

        if (request.expenseDate() != null) {
            expense.setExpenseDate(request.expenseDate());
        }
        if (request.storeName() != null && !request.storeName().isBlank()) {
            expense.setStoreName(storeNameNormalizer.normalize(request.storeName()));
        }
        if (request.amount() != null) {
            expense.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.category() != null && !request.category().isBlank()) {
            expense.setCategory(categoryService.resolveCategoryCode(request.category()));
        }
        if (request.subcategory() != null) {
            expense.setSubcategory(normalizeNullableText(request.subcategory()));
        }
        if (request.description() != null) {
            expense.setDescription(normalizeNullableText(request.description()));
        }
        if (request.recurring() != null) {
            expense.setRecurring(request.recurring());
        }
        if (request.shared() != null) {
            expense.setShared(request.shared());
        }

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forExpenseId(id));
        expenseRepository.delete(expense);
    }

    @Override
    public ExpenseSummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        return getSummary(startDate, endDate, false);
    }

    @Override
    public ExpenseSummaryResponse getSummary(LocalDate startDate, LocalDate endDate, boolean splitShared) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La data di inizio non può essere successiva alla data di fine");
        }

        ExpenseFilterCriteria criteria = new ExpenseFilterCriteria(
                startDate,
                endDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<Expense> expenses = expenseRepository.findAll(
                ExpenseSpecifications.withCriteria(criteria),
                Sort.by(Sort.Direction.ASC, "expenseDate")
        );

        int totalCount = expenses.size();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> amountByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> amountByStore = new LinkedHashMap<>();

        YearMonth currentYearMonth = YearMonth.now();
        BigDecimal currentMonthTotalAmount = BigDecimal.ZERO;

        for (Expense expense : expenses) {
            BigDecimal effectiveAmount = calculateEffectiveAmount(expense, splitShared);
            totalAmount = totalAmount.add(effectiveAmount);

            String categoryKey = expense.getCategory();
            amountByCategory.merge(categoryKey, effectiveAmount, BigDecimal::add);

            String storeKey = expense.getStoreName();
            amountByStore.merge(storeKey, effectiveAmount, BigDecimal::add);

            YearMonth expenseYearMonth = YearMonth.from(expense.getExpenseDate());
            if (expenseYearMonth.equals(currentYearMonth)) {
                currentMonthTotalAmount = currentMonthTotalAmount.add(effectiveAmount);
            }
        }

        BigDecimal averageAmount = totalCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal averageMonthlyAmount = BigDecimal.ZERO;
        if (!expenses.isEmpty()) {
            LocalDate firstDate = expenses.getFirst().getExpenseDate();
            LocalDate lastDate = expenses.getLast().getExpenseDate();
            long monthsBetween = (lastDate.getYear() - firstDate.getYear()) * 12L
                    + (lastDate.getMonthValue() - firstDate.getMonthValue()) + 1L;
            long divisor = Math.max(1, monthsBetween);
            averageMonthlyAmount = totalAmount.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
        }

        return new ExpenseSummaryResponse(
                startDate,
                endDate,
                totalCount,
                totalAmount,
                averageAmount,
                averageMonthlyAmount,
                currentMonthTotalAmount,
                amountByCategory,
                amountByStore
        );
    }

    private BigDecimal calculateEffectiveAmount(Expense expense, boolean splitShared) {
        if (splitShared && expense.isShared()) {
            return expense.getAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        return expense.getAmount();
    }

    @Override
    public List<CategoryMetadataResponse> getCategories() {
        return categoryService.getAllCategories();
    }

    private ExpenseResponse toResponse(Expense expense) {
        String displayName = categoryService.getDisplayNameForCode(expense.getCategory());
        return ExpenseResponse.fromEntity(expense, displayName);
    }

    private void applyCreateRequest(Expense expense, ExpenseCreateRequest request) {
        expense.setExpenseDate(request.expenseDate());
        expense.setStoreName(storeNameNormalizer.normalize(request.storeName()));
        expense.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        expense.setCategory(categoryService.resolveCategoryCode(request.category()));
        expense.setSubcategory(normalizeNullableText(request.subcategory()));
        expense.setDescription(normalizeNullableText(request.description()));
        expense.setRecurring(Boolean.TRUE.equals(request.recurring()));
        expense.setShared(Boolean.TRUE.equals(request.shared()));
    }

    private String normalizeNullableText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private void validateDateAndAmountRange(ExpenseFilterCriteria criteria) {
        if (criteria == null) {
            return;
        }
        if (criteria.startDate() != null && criteria.endDate() != null
                && criteria.startDate().isAfter(criteria.endDate())) {
            throw new IllegalArgumentException("La data di inizio (startDate) non può essere successiva alla data di fine (endDate)");
        }
        if (criteria.minAmount() != null && criteria.maxAmount() != null
                && criteria.minAmount().compareTo(criteria.maxAmount()) > 0) {
            throw new IllegalArgumentException("L'importo minimo (minAmount) non può essere superiore all'importo massimo (maxAmount)");
        }
    }
}
