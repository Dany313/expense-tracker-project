package com.daniele.receiptocr.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

@Component
public class SpringAiReceiptVisionExtractor implements ReceiptVisionExtractor {

    private final ChatClient chatClient;
    private final CategoryProvider categoryProvider;

    public SpringAiReceiptVisionExtractor(ChatClient.Builder chatClientBuilder, CategoryProvider categoryProvider) {
        this.categoryProvider = categoryProvider;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public RawVisionReceiptResult extractFromImage(byte[] imageBytes, MimeType mimeType) {
        String categoriesHint = categoryProvider.getCategoriesHint();
        String categoryInstruction = categoriesHint.isBlank()
                ? "5. category: estrai la categoria della spesa in formato sintetico MAIUSCOLO (es. SPESA, RISTORANTE, BENZINA, FARMACIA)."
                : "5. category: scegli la categoria più pertinente tra quelle configurate nel sistema: " + categoriesHint + ".";

        String dynamicSystemPrompt = """
                Sei un sistema esperto di OCR finanziario specializzato nell'analisi di foto di scontrini fiscali italiani ed europei.
                Analizza l'immagine dello scontrino fornita ed estrai esclusivamente i dati generali della spesa
                (NON estrarre l'elenco dei singoli prodotti acquistati).

                REGOLE DI ESTRAZIONE:
                1. validReceipt: imposta a true se l'immagine mostra uno scontrino o ricevuta di pagamento leggibile, altrimenti false e compila errorMessage.
                2. expenseDate: estrai la data dello scontrino e restituiscila nel formato ISO YYYY-MM-DD (es. "2026-10-19").
                   Attenzione al formato italiano GG/MM/AAAA o GG-MM-AA sullo scontrino.
                3. storeName: estrai il nome dell'insegna/negozio (es. CONAD, ALDI, TIGOTA, FAMILA, EUROSPIN, ESSELUNGA, HAPPYCASA, COOP, LIDL).
                   Se trovi un'insegna riconducibile a una catena (es. "SPAZIO CONAD" o "MARGHERITA" -> "CONAD", "GOTTARDO" -> "TIGOTA", "MEGAMARK" -> "FAMILA"),
                   restituisci il nome principale della catena in MAIUSCOLO.
                4. amount: estrai l'importo TOTALE EFFETTIVAMENTE PAGATO (es. 38.50). Non confonderlo con il contante versato, il resto o i subtotali.
                """ + categoryInstruction + """
                
                6. subcategory: suggerisci una sottocategoria sintetica opzionale (es. "Spesa supermercato", "Igiene casa e persona", "Ristorante", "Farmacia") oppure null.
                7. description: breve sintesi opzionale (es. "Scontrino CONAD").
                """;

        ByteArrayResource imageResource = new ByteArrayResource(imageBytes);
        return chatClient.prompt()
                .system(dynamicSystemPrompt)
                .user(userSpec -> userSpec
                        .text("Analizza questa foto dello scontrino ed estrai i dati della spesa, del negozio e della data.")
                        .media(mimeType, imageResource))
                .call()
                .entity(RawVisionReceiptResult.class);
    }
}
