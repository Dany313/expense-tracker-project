package com.daniele.expensetracker.service;

import com.daniele.expensetracker.dto.CategoryMetadataResponse;
import com.daniele.expensetracker.dto.CategoryRequest;
import java.util.List;

public interface CategoryService {

    List<CategoryMetadataResponse> getAllCategories();

    CategoryMetadataResponse getCategoryByCode(String code);

    CategoryMetadataResponse createCategory(CategoryRequest request);

    CategoryMetadataResponse updateCategory(String code, CategoryRequest request);

    void deleteCategory(String code);

    String resolveCategoryCode(String rawInput);

    String getDisplayNameForCode(String code);
}
