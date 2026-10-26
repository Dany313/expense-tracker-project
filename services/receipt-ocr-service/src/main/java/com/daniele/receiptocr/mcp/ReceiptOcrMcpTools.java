package com.daniele.receiptocr.mcp;

import com.daniele.receiptocr.dto.ReceiptBase64Request;
import com.daniele.receiptocr.dto.ReceiptExtractionResponse;
import com.daniele.receiptocr.dto.ReceiptOcrJobStatusResponse;
import com.daniele.receiptocr.exception.InvalidReceiptImageException;
import com.daniele.receiptocr.messaging.ReceiptOcrQueueService;
import com.daniele.receiptocr.service.ReceiptOcrService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ReceiptOcrMcpTools {

    private final ReceiptOcrService receiptOcrService;
    private final ReceiptOcrQueueService receiptOcrQueueService;

    public ReceiptOcrMcpTools(
            @Lazy ReceiptOcrService receiptOcrService,
            @Lazy ReceiptOcrQueueService receiptOcrQueueService
    ) {
        this.receiptOcrService = receiptOcrService;
        this.receiptOcrQueueService = receiptOcrQueueService;
    }

    @Tool(
            name = "extract_expense_from_receipt_base64",
            description = """
                    Analizza la foto di uno scontrino codificata in Base64 ed estrae i dati della spesa:
                    data (expenseDate), negozio (storeName), importo totale (amount),
                    categoria suggerita (category), sottocategoria opzionale (subcategory) e descrizione (description).
                    Il formato restituito è già pronto per chiamare il tool create_expense di expense-service.
                    """
    )
    public ReceiptExtractionResponse extractExpenseFromReceiptBase64(
            @ToolParam(description = "Immagine dello scontrino codificata in Base64")
            String base64Image,
            @ToolParam(description = "MIME type opzionale dell'immagine (es. image/jpeg, image/png, image/webp). Default: image/jpeg", required = false)
            String mimeType
    ) {
        return receiptOcrService.extractFromBase64(new ReceiptBase64Request(base64Image, mimeType));
    }

    @Tool(
            name = "extract_expense_from_receipt_file_path",
            description = """
                    Analizza la foto di uno scontrino a partire da un percorso file locale sul filesystem
                    ed estrae data, negozio e dati della spesa nel formato richiesto da expense-service.
                    """
    )
    public ReceiptExtractionResponse extractExpenseFromReceiptFilePath(
            @ToolParam(description = "Percorso assoluto o relativo del file immagine dello scontrino")
            String filePath
    ) {
        if (filePath == null || filePath.isBlank()) {
            throw new InvalidReceiptImageException("Il percorso del file immagine non può essere vuoto");
        }
        try {
            Path path = Path.of(filePath.trim());
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new InvalidReceiptImageException("File immagine non trovato al percorso: " + filePath);
            }
            byte[] bytes = Files.readAllBytes(path);
            String probedContentType = Files.probeContentType(path);
            return receiptOcrService.extractFromBytes(bytes, probedContentType != null ? probedContentType : "image/jpeg");
        } catch (IOException e) {
            throw new InvalidReceiptImageException("Impossibile leggere il file immagine: " + e.getMessage(), e);
        }
    }

    @Tool(
            name = "enqueue_receipt_ocr_base64",
            description = """
                    Accoda l'analisi OCR di uno scontrino (Base64) su RabbitMQ per elaborazione asincrona resiliente
                    a picchi di carico di Gemini API e inoltro automatico della spesa a expense-service tramite coda.
                    """
    )
    public ReceiptOcrJobStatusResponse enqueueReceiptOcrBase64(
            @ToolParam(description = "Immagine dello scontrino codificata in Base64")
            String base64Image,
            @ToolParam(description = "MIME type opzionale dell'immagine (es. image/jpeg, image/png). Default: image/jpeg", required = false)
            String mimeType,
            @ToolParam(description = "Se true (default), salva automaticamente la spesa accodandola su expense.create.queue", required = false)
            Boolean autoSaveExpense
    ) {
        boolean autoSave = autoSaveExpense == null || autoSaveExpense;
        return receiptOcrQueueService.enqueueBase64(new ReceiptBase64Request(base64Image, mimeType), autoSave);
    }
}
