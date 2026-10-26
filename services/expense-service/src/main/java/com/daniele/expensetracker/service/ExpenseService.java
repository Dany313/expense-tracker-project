package com.daniele.expensetracker.service;

import com.daniele.expensetracker.dto.CategoryMetadataResponse;
import com.daniele.expensetracker.dto.ExpenseCreateRequest;
import com.daniele.expensetracker.dto.ExpenseFilterCriteria;
import com.daniele.expensetracker.dto.ExpenseResponse;
import com.daniele.expensetracker.dto.ExpenseSummaryResponse;
import com.daniele.expensetracker.dto.ExpenseUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseCreateRequest request);

    ExpenseResponse getExpenseById(UUID id);

    Page<ExpenseResponse> searchExpenses(ExpenseFilterCriteria criteria, Pageable pageable);

    List<ExpenseResponse> listExpenses(ExpenseFilterCriteria criteria, int limit);

    ExpenseResponse replaceExpense(UUID id, ExpenseCreateRequest request);

    ExpenseResponse updateExpense(UUID id, ExpenseUpdateRequest request);

    void deleteExpense(UUID id);

    ExpenseSummaryResponse getSummary(LocalDate startDate, LocalDate endDate);

    ExpenseSummaryResponse getSummary(LocalDate startDate, LocalDate endDate, boolean splitShared);

    List<CategoryMetadataResponse> getCategories();
}
