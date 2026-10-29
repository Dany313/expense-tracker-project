package com.daniele.expensetracker.service;

import com.daniele.expensetracker.domain.Category;
import com.daniele.expensetracker.dto.CategoryMetadataResponse;
import com.daniele.expensetracker.dto.CategoryRequest;
import com.daniele.expensetracker.exception.ResourceNotFoundException;
import com.daniele.expensetracker.repository.CategoryRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryMetadataResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryMetadataResponse getCategoryByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Il codice categoria non può essere vuoto");
        }
        return categoryRepository.findByCodeIgnoreCase(code.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria non trovata con codice: " + code));
    }

    @Override
    @Transactional
    public CategoryMetadataResponse createCategory(CategoryRequest request) {
        String normalizedCode = normalizeCode(request.code());
        if (categoryRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Esiste già una categoria con il codice: " + normalizedCode);
        }
        Category category = new Category(
                UUID.randomUUID(),
                normalizedCode,
                request.displayName().trim(),
                normalizeNullable(request.synonyms())
        );
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryMetadataResponse updateCategory(String code, CategoryRequest request) {
        Category category = categoryRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria non trovata con codice: " + code));

        if (request.displayName() != null && !request.displayName().isBlank()) {
            category.setDisplayName(request.displayName().trim());
        }
        if (request.synonyms() != null) {
            category.setSynonyms(normalizeNullable(request.synonyms()));
        }
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(String code) {
        Category category = categoryRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria non trovata con codice: " + code));
        categoryRepository.delete(category);
    }

    @Override
    public String resolveCategoryCode(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new IllegalArgumentException("La categoria è obbligatoria");
        }
        String trimmed = rawInput.trim();
        String normalized = trimmed.toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        List<Category> allCategories = categoryRepository.findAll();

        // 1. Corrispondenza esatta su codice
        for (Category cat : allCategories) {
            if (cat.getCode().equalsIgnoreCase(normalized) || cat.getCode().equalsIgnoreCase(trimmed)) {
                return cat.getCode();
            }
        }

        // 2. Corrispondenza esatta su nome visualizzato
        for (Category cat : allCategories) {
            if (cat.getDisplayName().equalsIgnoreCase(trimmed)) {
                return cat.getCode();
            }
        }

        // 3. Corrispondenza su sinonimi configurati a database
        for (Category cat : allCategories) {
            if (cat.getSynonyms() != null && !cat.getSynonyms().isBlank()) {
                String[] tokens = cat.getSynonyms().split(",");
                for (String token : tokens) {
                    String syn = token.trim();
                    if (syn.equalsIgnoreCase(trimmed)
                            || syn.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_').equals(normalized)) {
                        return cat.getCode();
                    }
                }
            }
        }

        // 4. Se non trovata, lancia eccezione con elenco dinamico delle categorie a database
        String validCategories = allCategories.stream()
                .map(c -> c.getCode() + " (" + c.getDisplayName() + ")")
                .collect(Collectors.joining(", "));

        throw new IllegalArgumentException(
                "Categoria non valida: '" + rawInput + "'. Categorie disponibili a database: [" + validCategories + "]"
        );
    }

    @Override
    public String getDisplayNameForCode(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        return categoryRepository.findByCodeIgnoreCase(code.trim())
                .map(Category::getDisplayName)
                .orElse(code);
    }

    private CategoryMetadataResponse toResponse(Category category) {
        return new CategoryMetadataResponse(
                category.getCode(),
                category.getDisplayName(),
                category.getSynonyms()
        );
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Il codice categoria non può essere vuoto");
        }
        return code.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private String normalizeNullable(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
