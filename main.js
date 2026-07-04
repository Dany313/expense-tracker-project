function doPost(e) {
  const logger = new Logger();
  const gemini = new GeminiClient();
  const telegramClient = new TelegramClient();

  logger.log("INFO", "NUOVA ESECUZIONE");

  try {
    const payload = JSON.parse(e.postData.contents);

    // --- SEZIONE TELEGRAM ---
    if (payload.update_id) {
      if (payload.message && payload.message.text) {
        if (payload.message.chat.id.toString() !== telegramClient.chatId) {
          // Usa HtmlService anche qui per sicurezza
          return HtmlService.createHtmlOutput(""); 
        }
        
        telegramClient.handleCommand(payload.message.text);
      }
      
      // LA SOLUZIONE DI REDDIT:
      return HtmlService.createHtmlOutput(""); 
    }

    // 2. ALTRIMENTI, È MACRODROID
    const testoNotifica = payload.testo; 
    if (!testoNotifica) throw new Error("Payload sconosciuto (testo mancante)");
    logger.log("INFO", `Notifica in ingresso: ${testoNotifica}`);
    
    // 3. Elaborazione Business Logic
    const extractedData = gemini.extractExpenseData(testoNotifica);
    logger.log("SUCCESS", `Dati estratti: ${JSON.stringify(extractedData)}`);
    
    // 4. Scrittura e formattazione data
    const dataOggi = Utilities.formatDate(new Date(), "GMT+02:00", "dd/MM/yyyy");
    logger.saveExpense(dataOggi, extractedData.negozio, extractedData.importo);
    logger.log("INFO", "Nuova spesa aggiunta al foglio principale!");
    
    // 5. Risposta HTTP

    telegramClient.sendMessage(`✅ <b>Nuova Spesa Salvata!</b>\nNegozio: ${extractedData.negozio}\nImporto: ${extractedData.importo} €`);

    return ContentService.createTextOutput(`Successo: ${extractedData.negozio}`).setMimeType(ContentService.MimeType.TEXT);
    
  } catch (error) {
    logger.log("ERROR", `Errore Critico: ${error.toString()}`);
    telegramClient.sendMessage(`⚠️ <b>Errore di sistema:</b>\n${error.toString()}`);
    return ContentService.createTextOutput(`Errore: ${error.toString()}`).setMimeType(ContentService.MimeType.TEXT);
  }
}