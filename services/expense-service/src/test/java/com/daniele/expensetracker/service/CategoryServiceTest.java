package com.daniele.expensetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daniele.expensetracker.domain.Category;
import com.daniele.expensetracker.dto.CategoryMetadataResponse;
import com.daniele.expensetracker.dto.CategoryRequest;
import com.daniele.expensetracker.exception.ResourceNotFoundException;
import com.daniele.expensetracker.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository);
    }

    @Test
    @DisplayName("resolveCategoryCode risolve per codice esatto, displayName o sinonimi a database")
    void shouldResolveCategoryCodeFromDatabase() {
        Category alimentari = new Category(
                UUID.randomUUID(),
                "SPESA_ALIMENTARE_E_CASA",
                "Spesa alimentare e casa",
                "spesa, supermercato, cibo, alimentari"
        );
        Category svago = new Category(
                UUID.randomUUID(),
                "SVAGO",
                "Svago",
                "cinema, giochi, videogiochi"
        );

        when(categoryRepository.findAll()).thenReturn(List.of(alimentari, svago));

        // Corrispondenza codice
        assertThat(categoryService.resolveCategoryCode("SPESA_ALIMENTARE_E_CASA")).isEqualTo("SPESA_ALIMENTARE_E_CASA");
        assertThat(categoryService.resolveCategoryCode("spesa_alimentare_e_casa")).isEqualTo("SPESA_ALIMENTARE_E_CASA");

        // Corrispondenza displayName
        assertThat(categoryService.resolveCategoryCode("Spesa alimentare e casa")).isEqualTo("SPESA_ALIMENTARE_E_CASA");

        // Corrispondenza sinonimo
        assertThat(categoryService.resolveCategoryCode("supermercato")).isEqualTo("SPESA_ALIMENTARE_E_CASA");
        assertThat(categoryService.resolveCategoryCode("cinema")).isEqualTo("SVAGO");
    }

    @Test
    @DisplayName("resolveCategoryCode lancia eccezione se la categoria non è presente a database")
    void shouldThrowIfCategoryNotFound() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> categoryService.resolveCategoryCode("CATEGORIA_SCONOSCIUTA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria non valida: 'CATEGORIA_SCONOSCIUTA'");
    }

    @Test
    @DisplayName("createCategory salva nuova categoria e normalizza il codice")
    void shouldCreateNewCategory() {
        when(categoryRepository.existsByCodeIgnoreCase("NUOVA_CAT")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryRequest request = new CategoryRequest("nuova cat", "Nuova Categoria", "test, prova");
        CategoryMetadataResponse response = categoryService.createCategory(request);

        assertThat(response.code()).isEqualTo("NUOVA_CAT");
        assertThat(response.displayName()).isEqualTo("Nuova Categoria");
        assertThat(response.synonyms()).isEqualTo("test, prova");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("deleteCategory rimuove la categoria dal database")
    void shouldDeleteCategory() {
        Category cat = new Category(UUID.randomUUID(), "TEST", "Test", null);
        when(categoryRepository.findByCodeIgnoreCase("TEST")).thenReturn(Optional.of(cat));

        categoryService.deleteCategory("TEST");

        verify(categoryRepository).delete(cat);
    }
}
