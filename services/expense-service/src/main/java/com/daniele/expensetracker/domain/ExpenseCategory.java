package com.daniele.expensetracker.domain;

import java.util.Arrays;
import java.util.Locale;

public enum ExpenseCategory {
    GROCERIES("Alimentari e Supermercato"),
    DINING("Ristorazione, Bar e Delivery"),
    TRANSPORT("Trasporti, Carburante e Auto"),
    HOUSING("Casa, Affitto e Manutenzione"),
    BILLS_AND_UTILITIES("Bollette, Utenze e Tasse"),
    HEALTH("Salute, Farmacia e Visite Mediche"),
    PERSONAL_CARE("Cura della Persona e Igiene Casa"),
    SHOPPING("Acquisti, Abbigliamento ed Elettronica"),
    ENTERTAINMENT("Svago, Abbonamenti e Tempo Libero"),
    TRAVEL("Viaggi e Vacanze"),
    EDUCATION("Istruzione, Libri e Formazione"),
    OTHER("Altro");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ExpenseCategory fromFlexibleString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("La categoria non può essere vuota. Valori ammessi: " + Arrays.toString(values()));
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        for (ExpenseCategory category : values()) {
            if (category.name().equals(normalized) || category.displayName.equalsIgnoreCase(raw.trim())) {
                return category;
            }
        }

        return switch (normalized) {
            case "ALIMENTARI", "SUPERMERCATO", "SPESA", "CIBO" -> GROCERIES;
            case "RISTORANTE", "RISTORAZIONE", "BAR", "PIZZERIA", "DELIVERY" -> DINING;
            case "TRASPORTI", "AUTO", "CARBURANTE", "BENZINA", "MEZZI" -> TRANSPORT;
            case "CASA", "AFFITTO", "MUTUO", "ARREDAMENTO" -> HOUSING;
            case "BOLLETTE", "UTENZE", "TASSE" -> BILLS_AND_UTILITIES;
            case "SALUTE", "FARMACIA", "MEDICO", "SPESE_MEDICHE" -> HEALTH;
            case "CURA_PERSONA", "IGIENE", "DETERSIVI", "COSMETICI" -> PERSONAL_CARE;
            case "ACQUISTI", "ABBIGLIAMENTO", "ELETTRONICA", "TECH" -> SHOPPING;
            case "SVAGO", "INTRATTENIMENTO", "ABBONAMENTI", "HOBBY", "SPORT" -> ENTERTAINMENT;
            case "VIAGGI", "VACANZE", "HOTEL", "VOLI" -> TRAVEL;
            case "ISTRUZIONE", "LIBRI", "CORSI", "FORMAZIONE" -> EDUCATION;
            case "ALTRO", "VARIE" -> OTHER;
            default -> throw new IllegalArgumentException(
                    "Categoria non valida: '" + raw + "'. Valori supportati: " + Arrays.toString(values())
            );
        };
    }
}
