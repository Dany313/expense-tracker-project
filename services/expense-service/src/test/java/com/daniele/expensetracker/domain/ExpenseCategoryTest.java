package com.daniele.expensetracker.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpenseCategoryTest {

    @Test
    @DisplayName("Dovrebbe contenere tutte le 11 categorie previste con i rispettivi displayName")
    void shouldHaveAllExpectedCategories() {
        assertThat(ExpenseCategory.values()).hasSize(11);
        assertThat(ExpenseCategory.AFFITTO.getDisplayName()).isEqualTo("Affitto");
        assertThat(ExpenseCategory.BOLLETTE.getDisplayName()).isEqualTo("Bollette");
        assertThat(ExpenseCategory.CARBURANTE_E_TRASPORTI.getDisplayName()).isEqualTo("Carburante e trasporti");
        assertThat(ExpenseCategory.MANUTENZIONE_AUTO_ORDINARIA.getDisplayName()).isEqualTo("Manutenzione auto ordinaria");
        assertThat(ExpenseCategory.MANUTENZIONE_AUTO_STRAORDINARIA.getDisplayName()).isEqualTo("Manutenzione auto straordinaria");
        assertThat(ExpenseCategory.SPESA_ALIMENTARE_E_CASA.getDisplayName()).isEqualTo("Spesa alimentare e casa");
        assertThat(ExpenseCategory.VIAGGI.getDisplayName()).isEqualTo("Viaggi");
        assertThat(ExpenseCategory.SVAGO.getDisplayName()).isEqualTo("Svago");
        assertThat(ExpenseCategory.USCITE_FUORI.getDisplayName()).isEqualTo("Uscite fuori");
        assertThat(ExpenseCategory.SALUTE_E_SPORT.getDisplayName()).isEqualTo("Salute e sport");
        assertThat(ExpenseCategory.ABBIGLIAMENTO.getDisplayName()).isEqualTo("Abbigliamento");
    }

    @ParameterizedTest
    @CsvSource({
            "AFFITTO, AFFITTO",
            "affitto, AFFITTO",
            "mutuo, AFFITTO",
            "canone, AFFITTO",
            "bollette, BOLLETTE",
            "luce, BOLLETTE",
            "gas, BOLLETTE",
            "benzina, CARBURANTE_E_TRASPORTI",
            "carburante, CARBURANTE_E_TRASPORTI",
            "tagliando, MANUTENZIONE_AUTO_ORDINARIA",
            "meccanico, MANUTENZIONE_AUTO_STRAORDINARIA",
            "spesa, SPESA_ALIMENTARE_E_CASA",
            "supermercato, SPESA_ALIMENTARE_E_CASA",
            "viaggi, VIAGGI",
            "hotel, VIAGGI",
            "svago, SVAGO",
            "cinema, SVAGO",
            "uscite fuori, USCITE_FUORI",
            "USCITE_FUORI, USCITE_FUORI",
            "ristorante, USCITE_FUORI",
            "pizzeria, USCITE_FUORI",
            "bar, USCITE_FUORI",
            "aperitivo, USCITE_FUORI",
            "cena, USCITE_FUORI",
            "pranzo, USCITE_FUORI",
            "palestra, SALUTE_E_SPORT",
            "farmacia, SALUTE_E_SPORT",
            "abbigliamento, ABBIGLIAMENTO",
            "vestiti, ABBIGLIAMENTO"
    })
    @DisplayName("fromFlexibleString risolve correttamente i sinonimi e i codici")
    void shouldResolveFlexibleStrings(String input, ExpenseCategory expected) {
        assertThat(ExpenseCategory.fromFlexibleString(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("fromFlexibleString lancia eccezione per stringhe nulle, vuote o non valide")
    void shouldThrowOnInvalidCategory() {
        assertThatThrownBy(() -> ExpenseCategory.fromFlexibleString(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpenseCategory.fromFlexibleString("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpenseCategory.fromFlexibleString("CATEGORIA_INESISTENTE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
