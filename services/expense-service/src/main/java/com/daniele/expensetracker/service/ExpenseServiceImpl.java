package com.daniele.expensetracker.service;

import com.daniele.expensetracker.domain.Expense;
import com.daniele.expensetracker.domain.ExpenseCategory;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

    public ExpenseServiceImpl(ExpenseRepository expenseRepository, StoreNameNormalizer storeNameNormalizer) {
        this.expenseRepository = expenseRepository;
        this.storeNameNormalizer = storeNameNormalizer;
    }

    @Override
    @Transactional
    public ExpenseResponse createExpense(ExpenseCreateRequest request) {
        Expense expense = new Expense();
        applyCreateRequest(expense, request);
        Expense saved = expenseRepository.save(expense);
        return ExpenseResponse.fromEntity(saved);
    }

    @Override
    public ExpenseResponse getExpenseById(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forExpenseId(id));
        return ExpenseResponse.fromEntity(expense);
    }

    @Override
    public Page<ExpenseResponse> searchExpenses(ExpenseFilterCriteria criteria, Pageable pageable) {
        validateDateAndAmountRange(criteria);
        return expenseRepository.findAll(ExpenseSpecifications.withCriteria(criteria), pageable)
                .map(ExpenseResponse::fromEntity);
    }

    @Override
    public List<ExpenseResponse> listExpenses(ExpenseFilterCriteria criteria, int limit) {
        validateDateAndAmountRange(criteria);
        int effectiveLimit = Math.clamp(limit <= 0 ? 50 : limit, 1, MAX_LIST_LIMIT);
        Pageable pageable = PageRequest.of(
                0,
                effectiveLimit,
                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("createdAt"))
        );
        return expenseRepository.findAll(ExpenseSpecifications.withCriteria(criteria), pageable)
                .stream()
                .map(ExpenseResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public ExpenseResponse replaceExpense(UUID id, ExpenseCreateRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forExpenseId(id));
        applyCreateRequest(expense, request);
        Expense saved = expenseRepository.save(expense);
        return ExpenseResponse.fromEntity(saved);
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
        if (request.category() != null) {
            expense.setCategory(request.category());
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
        return ExpenseResponse.fromEntity(saved);
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
            throw new IllegalArgumentException("La data di inizio (startDate) non può essere successiva alla data di fine (endDate)");
        }

        ExpenseFilterCriteria criteria = new ExpenseFilterCriteria(
                startDate, endDate, null, null, null, null, null, null, null
        );
        List<Expense> expenses = expenseRepository.findAll(ExpenseSpecifications.withCriteria(criteria));

        long totalCount = expenses.size();
        BigDecimal totalAmount = expenses.stream()
                .map(e -> calculateEffectiveAmount(e, splitShared))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal averageAmount = totalCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        long distinctMonths = expenses.stream()
                .map(e -> YearMonth.from(e.getExpenseDate()))
                .distinct()
                .count();

        BigDecimal averageMonthlyAmount = distinctMonths > 0
                ? totalAmount.divide(BigDecimal.valueOf(distinctMonths), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        YearMonth currentYearMonth = YearMonth.now();
        List<Expense> currentMonthExpenses = (startDate == null && endDate == null)
                ? expenses.stream()
                        .filter(e -> YearMonth.from(e.getExpenseDate()).equals(currentYearMonth))
                        .toList()
                : expenseRepository.findAll(ExpenseSpecifications.withCriteria(new ExpenseFilterCriteria(
                        currentYearMonth.atDay(1),
                        currentYearMonth.atEndOfMonth(),
                        null, null, null, null, null, null, null
                )));

        BigDecimal currentMonthTotalAmount = currentMonthExpenses.stream()
                .map(e -> calculateEffectiveAmount(e, splitShared))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> amountByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, e -> calculateEffectiveAmount(e, splitShared), BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().setScale(2, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<String, BigDecimal> amountByStore = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getStoreName,
                        Collectors.reducing(BigDecimal.ZERO, e -> calculateEffectiveAmount(e, splitShared), BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().setScale(2, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

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
        return Arrays.stream(ExpenseCategory.values())
                .map(c -> new CategoryMetadataResponse(c.name(), c.getDisplayName()))
                .toList();
    }

    private void applyCreateRequest(Expense expense, ExpenseCreateRequest request) {
        expense.setExpenseDate(request.expenseDate());
        expense.setStoreName(storeNameNormalizer.normalize(request.storeName()));
        expense.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        expense.setCategory(request.category());
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
