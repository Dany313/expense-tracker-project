class MacroDroidClient {
  constructor(geminiClient, spreadSheetService, logger) {
    this.gemini = geminiClient;
    this.db = spreadSheetService;
    this.logger = logger;
  }

  handleRequest(payload) {
    const testoNotifica = payload.testo;
    if (!testoNotifica) {
      this.logger.debug("Ricevuto payload MacroDroid senza testo");
      return { success: null, error: "Payload MacroDroid senza campo 'testo'" };
    }

    this.logger.info(`Notifica in ingresso da MacroDroid: ${testoNotifica}`);

    // Elaborazione Gemini
    const extractedData = this.gemini.extractExpenseData(testoNotifica);

    if (extractedData.error) {
      this.logger.debug(`Gemini ha restituito un errore per la notifica MacroDroid: ${extractedData.error}`);
      return { success: null, error: `<b>Errore di elaborazione:</b>\n${extractedData.error}` };
    }

    this.logger.debug(`Estrazione riuscita: Negozio=${extractedData.negozio}, Importo=${extractedData.importo}`);

    // Scrittura sul foglio
    const dataOggi = Utilities.formatDate(new Date(), "GMT+02:00", "dd/MM/yyyy");
    this.db.appendExpense(dataOggi, extractedData.negozio, extractedData.importo);
    
    this.logger.info("Spesa da MacroDroid registrata sul foglio principale");

    return { success: `<b>Nuova Spesa Salvata!</b>\nNegozio: ${extractedData.negozio}\nImporto: ${extractedData.importo} €`, error: null };
  }
}
