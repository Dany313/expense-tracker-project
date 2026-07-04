class MacroDroidClient extends IClientHandler {
  constructor() {
    super();
    this.logger = new Logger();
    this.gemini = new GeminiClient();
    this.telegram = new TelegramClient(); // Ci serve per la notifica finale
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
    this.logger.saveExpense(dataOggi, extractedData.negozio, extractedData.importo);
    this.logger.log("INFO", "Nuova spesa aggiunta al foglio principale!");
    
    // Notifica di conferma
    this.telegram.sendMessage(`✅ <b>Nuova Spesa Salvata!</b>\nNegozio: ${extractedData.negozio}\nImporto: ${extractedData.importo} €`);

    return ContentService.createTextOutput(`Successo: ${extractedData.negozio}`).setMimeType(ContentService.MimeType.TEXT);
  }
}
