package com.daniele.receiptocr.service;

import com.daniele.receiptocr.dto.ReceiptBase64Request;
import com.daniele.receiptocr.dto.ReceiptExtractionResponse;
import com.daniele.receiptocr.exception.InvalidReceiptImageException;
import com.daniele.receiptocr.exception.UnreadableReceiptException;
import com.daniele.receiptocr.service.ReceiptVisionExtractor.RawVisionReceiptResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReceiptOcrServiceImpl implements ReceiptOcrService {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "AFFITTO", "BOLLETTE", "CARBURANTE_E_TRASPORTI", "MANUTENZIONE_AUTO_ORDINARIA",
            "MANUTENZIONE_AUTO_STRAORDINARIA", "SPESA_ALIMENTARE_E_CASA", "VIAGGI", "SVAGO",
            "USCITE_FUORI", "SALUTE_E_SPORT", "ABBIGLIAMENTO"
    );

    private final ReceiptVisionExtractor visionExtractor;
    private final ReceiptStoreNormalizer storeNormalizer;

    public ReceiptOcrServiceImpl(ReceiptVisionExtractor visionExtractor, ReceiptStoreNormalizer storeNormalizer) {
        this.visionExtractor = visionExtractor;
        this.storeNormalizer = storeNormalizer;
    }

    @Override
    public ReceiptExtractionResponse extractFromMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidReceiptImageException("Il file immagine dello scontrino è vuoto o mancante");
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            return extractFromBytes(file.getBytes(), contentType);
        } catch (IOException e) {
            throw new InvalidReceiptImageException("Errore nella lettura del file immagine: " + e.getMessage(), e);
        }
    }

    @Override
    public ReceiptExtractionResponse extractFromBase64(ReceiptBase64Request request) {
        if (request == null || request.base64Image() == null || request.base64Image().isBlank()) {
            throw new InvalidReceiptImageException("Il contenuto Base64 dell'immagine è vuoto o mancante");
        }
        String rawBase64 = request.base64Image().trim();
        String detectedMime = request.mimeType();

        // Support optional data URI prefix: data:image/jpeg;base64,...
        if (rawBase64.startsWith("data:") && rawBase64.contains(",")) {
            int commaIndex = rawBase64.indexOf(',');
            String header = rawBase64.substring(5, commaIndex);
            if (header.contains(";")) {
                detectedMime = header.substring(0, header.indexOf(';'));
            }
            rawBase64 = rawBase64.substring(commaIndex + 1);
        }

        try {
            byte[] imageBytes = Base64.getMimeDecoder().decode(rawBase64);
            if (imageBytes.length == 0) {
                throw new InvalidReceiptImageException("L'immagine decodificata da Base64 è vuota");
            }
            return extractFromBytes(imageBytes, detectedMime != null ? detectedMime : "image/jpeg");
        } catch (IllegalArgumentException e) {
            throw new InvalidReceiptImageException("Stringa Base64 non valida: " + e.getMessage(), e);
        }
    }

    @Override
    public ReceiptExtractionResponse extractFromBytes(byte[] imageBytes, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new InvalidReceiptImageException("I byte dell'immagine dello scontrino sono vuoti");
        }

        String normalizedMime = (contentType == null || contentType.isBlank())
                ? "image/jpeg"
                : contentType.trim().toLowerCase(Locale.ROOT);

        if (!SUPPORTED_MIME_TYPES.contains(normalizedMime)) {
            throw new InvalidReceiptImageException(
                    "Formato immagine non supportato: '" + contentType + "'. Formati ammessi: " + SUPPORTED_MIME_TYPES
            );
        }

        MimeType mimeType = MimeTypeUtils.parseMimeType(
                normalizedMime.equals("image/jpg") ? "image/jpeg" : normalizedMime
        );

        RawVisionReceiptResult rawResult = visionExtractor.extractFromImage(imageBytes, mimeType);
        if (rawResult == null) {
            throw new UnreadableReceiptException("Il modello Vision non ha restituito alcun risultato per lo scontrino");
        }

        if (Boolean.FALSE.equals(rawResult.validReceipt())) {
            String reason = (rawResult.errorMessage() != null && !rawResult.errorMessage().isBlank())
                    ? rawResult.errorMessage()
                    : "L'immagine non sembra essere uno scontrino valido o non è leggibile";
            throw new UnreadableReceiptException(reason);
        }

        if (rawResult.amount() == null || rawResult.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new UnreadableReceiptException("Impossibile estrarre un importo totale valido (> 0) dallo scontrino");
        }

        LocalDate expenseDate = parseDateOrFallbackToday(rawResult.expenseDate());
        String normalizedStore = storeNormalizer.normalize(rawResult.storeName());
        BigDecimal normalizedAmount = rawResult.amount().setScale(2, RoundingMode.HALF_UP);
        String category = normalizeCategory(rawResult.category(), normalizedStore);
        String subcategory = normalizeNullable(rawResult.subcategory());
        String description = normalizeNullable(rawResult.description());

        return new ReceiptExtractionResponse(
                expenseDate,
                normalizedStore,
                normalizedAmount,
                category,
                subcategory,
                description,
                false
        );
    }

    private LocalDate parseDateOrFallbackToday(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private String normalizeCategory(String rawCategory, String normalizedStore) {
        if (rawCategory != null && !rawCategory.isBlank()) {
            String upper = rawCategory.trim().toUpperCase(Locale.ROOT);
            if (VALID_CATEGORIES.contains(upper)) {
                return upper;
            }
        }
        return "SPESA_ALIMENTARE_E_CASA";
    }

    private String normalizeNullable(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
