package com.daniele.expensetracker.repository;

import com.daniele.expensetracker.domain.Expense;
import com.daniele.expensetracker.dto.ExpenseFilterCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> withCriteria(ExpenseFilterCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), criteria.startDate()));
            }
            if (criteria.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), criteria.endDate()));
            }
            if (criteria.storeName() != null && !criteria.storeName().isBlank()) {
                String pattern = "%" + criteria.storeName().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("storeName")), pattern));
            }
            if (criteria.category() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.category()));
            }
            if (criteria.subcategory() != null && !criteria.subcategory().isBlank()) {
                String pattern = "%" + criteria.subcategory().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("subcategory")), pattern));
            }
            if (criteria.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), criteria.minAmount()));
            }
            if (criteria.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), criteria.maxAmount()));
            }
            if (criteria.recurring() != null) {
                predicates.add(cb.equal(root.get("recurring"), criteria.recurring()));
            }
            if (criteria.shared() != null) {
                predicates.add(cb.equal(root.get("shared"), criteria.shared()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
