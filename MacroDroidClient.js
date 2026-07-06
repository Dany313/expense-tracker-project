class MacroDroidClient {
  constructor(geminiClient, logger, spreadSheetService) {
    this.logger = logger;
    this.gemini = geminiClient;
    this.db = spreadSheetService;
  }

  handleRequest(payload) {
    const testoNotifica = payload.testo;
    if (!testoNotifica) throw new Error("Payload MacroDroid senza campo 'testo'");

    this.logger.log("INFO", `Notifica in ingresso da MacroDroid: ${testoNotifica}`);

    // Elaborazione Gemini
    const extractedData = this.gemini.extractExpenseData(testoNotifica);
    this.logger.log("SUCCESS", `Dati estratti: ${JSON.stringify(extractedData)}`);

    // Scrittura sul foglio
    const dataOggi = Utilities.formatDate(new Date(), "GMT+02:00", "dd/MM/yyyy");
    this.db.appendExpense(dataOggi, extractedData.negozio, extractedData.importo);
    this.logger.log("INFO", "Nuova spesa aggiunta al foglio principale!");
    this.logger.log("SUCCESS", `✅ <b>Nuova Spesa Salvata!</b>\nNegozio: ${extractedData.negozio}\nImporto: ${extractedData.importo} €`);

    // Notifica di conferma
    /*if (this.telegram) {
      this.telegram.sendMessage(`✅ <b>Nuova Spesa Salvata!</b>\nNegozio: ${extractedData.negozio}\nImporto: ${extractedData.importo} €`);
    }*/

    return ContentService.createTextOutput(`Successo: ${extractedData.negozio}`).setMimeType(ContentService.MimeType.TEXT);
  }
}
