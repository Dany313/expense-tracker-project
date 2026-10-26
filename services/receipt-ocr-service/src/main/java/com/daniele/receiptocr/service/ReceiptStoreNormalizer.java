package com.daniele.receiptocr.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReceiptStoreNormalizer {

    private static final Map<String, List<String>> KNOWN_STORE_ALIASES = new LinkedHashMap<>();

    static {
        KNOWN_STORE_ALIASES.put("ALDI", List.of("ALDI", "ALDI SUISSE"));
        KNOWN_STORE_ALIASES.put("CONAD", List.of("CONAD", "SPAZIO CONAD", "MARGHERITA", "CONAD SUPERSTORE"));
        KNOWN_STORE_ALIASES.put("FAMILA", List.of("FAMILA", "MAXI FAMILA", "MEGAMARK"));
        KNOWN_STORE_ALIASES.put("EUROSPIN", List.of("EUROSPIN", "EUROSPIN ITALIA"));
        KNOWN_STORE_ALIASES.put("TIGOTA", List.of("TIGOTA", "TIGOTÀ", "GOTTARDO"));
        KNOWN_STORE_ALIASES.put("ESSELUNGA", List.of("ESSELUNGA"));
        KNOWN_STORE_ALIASES.put("HAPPYCASA", List.of("HAPPYCASA", "HAPPY CASA"));
        KNOWN_STORE_ALIASES.put("COOP", List.of("COOP", "IPERCOOP", "COOP ALLEANZA"));
        KNOWN_STORE_ALIASES.put("LIDL", List.of("LIDL", "LIDL ITALIA"));
    }

    public String normalize(String rawStoreName) {
        if (rawStoreName == null || rawStoreName.isBlank()) {
            return "SCONOSCIUTO";
        }
        String cleaned = rawStoreName.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entry : KNOWN_STORE_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (cleaned.equals(alias) || cleaned.contains(alias)) {
                    return entry.getKey();
                }
            }
        }
        return cleaned;
    }

    public Map<String, List<String>> getKnownStoreAliases() {
        return Map.copyOf(KNOWN_STORE_ALIASES);
    }
}
