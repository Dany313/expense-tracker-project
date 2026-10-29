package com.daniele.expensetracker.domain;

import java.util.Arrays;
import java.util.Locale;

public enum ExpenseCategory {
    AFFITTO("Affitto"),
    BOLLETTE("Bollette"),
    CARBURANTE_E_TRASPORTI("Carburante e trasporti"),
    MANUTENZIONE_AUTO_ORDINARIA("Manutenzione auto ordinaria"),
    MANUTENZIONE_AUTO_STRAORDINARIA("Manutenzione auto straordinaria"),
    SPESA_ALIMENTARE_E_CASA("Spesa alimentare e casa"),
    VIAGGI("Viaggi"),
    SVAGO("Svago"),
    SALUTE_E_SPORT("Salute e sport"),
    ABBIGLIAMENTO("Abbigliamento");

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
            case "AFFITTO", "CASA_AFFITTO", "CANONE", "LOCAZIONE", "HOUSING", "MUTUO" -> AFFITTO;
            case "BOLLETTE", "UTENZE", "LUCE", "GAS", "ACQUA", "INTERNET", "BILLS_AND_UTILITIES", "BOLLETTA", "TARI" -> BOLLETTE;
            case "CARBURANTE", "BENZINA", "GASOLIO", "CARBURANTE_E_TRASPORTI", "TRASPORTI", "MEZZI", "TRENO", "BUS", "METRO", "AUTOSTRADA", "PEDAGGIO", "TRANSPORT" -> CARBURANTE_E_TRASPORTI;
            case "TAGLIANDO", "REVISIONE", "CAMBIO_OLIO", "MANUTENZIONE_ORDINARIA", "MANUTENZIONE_AUTO_ORDINARIA", "GOMME", "GOMMISTA" -> MANUTENZIONE_AUTO_ORDINARIA;
            case "GUASTO", "RIPARAZIONE", "CARROZZIERE", "MANUTENZIONE_STRAORDINARIA", "MANUTENZIONE_AUTO_STRAORDINARIA", "MECCANICO" -> MANUTENZIONE_AUTO_STRAORDINARIA;
            case "SPESA", "ALIMENTARI", "SUPERMERCATO", "CASA", "IGIENE", "DETERSIVI", "DROGHERIA", "SPESA_ALIMENTARE_E_CASA", "GROCERIES", "PERSONAL_CARE", "OTHER", "ALTRO" -> SPESA_ALIMENTARE_E_CASA;
            case "VIAGGI", "VACANZE", "HOTEL", "VOLI", "B&B", "AIRBNB", "TRAVEL", "VACANZA", "VIAGGIO" -> VIAGGI;
            case "SVAGO", "TEMPO_LIBERO", "RISTORANTE", "BAR", "PUB", "PIZZERIA", "CINEMA", "GIOCHI", "VIDEOGIOCHI", "ABBONAMENTI", "NETFLIX", "SPOTIFY", "ENTERTAINMENT", "DINING", "DELIVERY" -> SVAGO;
            case "SALUTE", "SPORT", "FARMACIA", "MEDICO", "VISITE", "PALESTRA", "PISCINA", "SALUTE_E_SPORT", "HEALTH" -> SALUTE_E_SPORT;
            case "ABBIGLIAMENTO", "VESTITI", "SCARPE", "MODA", "ACCESSORI", "SHOPPING" -> ABBIGLIAMENTO;
            default -> throw new IllegalArgumentException(
                    "Categoria non valida: '" + raw + "'. Valori supportati: " + Arrays.toString(values())
            );
        };
    }
}
