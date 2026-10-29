package com.daniele.receiptocr.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DynamicCategoryProvider implements CategoryProvider {

    private static final Logger log = LoggerFactory.getLogger(DynamicCategoryProvider.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final String categoriesUrl;

    private volatile List<String> cachedCodes = Collections.emptyList();
    private volatile String cachedHint = "";
    private volatile Instant lastFetch = Instant.EPOCH;

    public DynamicCategoryProvider(
            @Value("${expense.service.url:http://expense-service:8080}") String expenseServiceUrl
    ) {
        this.restClient = RestClient.builder().build();
        this.categoriesUrl = expenseServiceUrl.replaceAll("/+$", "") + "/api/v1/expenses/categories";
    }

    @Override
    public List<String> getAvailableCategoryCodes() {
        refreshCacheIfNeeded();
        return cachedCodes;
    }

    @Override
    public String getCategoriesHint() {
        refreshCacheIfNeeded();
        return cachedHint;
    }

    private synchronized void refreshCacheIfNeeded() {
        if (Instant.now().isBefore(lastFetch.plus(CACHE_TTL)) && !cachedCodes.isEmpty()) {
            return;
        }

        try {
            List<Map<String, Object>> response = restClient.get()
                    .uri(categoriesUrl)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response != null && !response.isEmpty()) {
                List<String> codes = new ArrayList<>();
                List<String> hints = new ArrayList<>();

                for (Map<String, Object> item : response) {
                    Object codeObj = item.get("code");
                    Object nameObj = item.get("displayName");
                    if (codeObj != null) {
                        String code = codeObj.toString().trim();
                        codes.add(code);
                        if (nameObj != null) {
                            hints.add(code + " (" + nameObj + ")");
                        } else {
                            hints.add(code);
                        }
                    }
                }

                if (!codes.isEmpty()) {
                    this.cachedCodes = List.copyOf(codes);
                    this.cachedHint = String.join(", ", hints);
                    this.lastFetch = Instant.now();
                    log.debug("Categorie aggiornate dinamicamente da expense-service: {}", this.cachedCodes);
                }
            }
        } catch (Exception e) {
            log.warn("Impossibile recuperare le categorie da expense-service ({}): {}", categoriesUrl, e.getMessage());
        }
    }
}
