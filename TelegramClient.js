class TelegramClient {
  constructor(spreadSheetService, geminiClient, logger) {
    this.chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID");
    this.db = spreadSheetService;
    this.gemini = geminiClient;
    this.logger = logger;

    this.commands = {
      "/totale": () => this._calcolaTotaleMese(),
      "/cancella": () => this._eliminaUltimaRiga()
    };
  }

  handleRequest(payload) {
    this.logger.debug("Inizio elaborazione payload Telegram");
    if (payload.message && payload.message.text) {
      if (this.chatId && payload.message.chat.id.toString() !== this.chatId) {
        this.logger.error(`Tentativo di accesso non autorizzato da chat ID: ${payload.message.chat.id}`);
        return { success: null, error: "Accesso non autorizzato" };
      }
      this.logger.info(`Ricevuto messaggio da Telegram: ${payload.message.text}`);

      // Se il testo non inizia con "/", assumiamo che sia un inserimento spesa libero
      if (!payload.message.text.startsWith("/")) {
        this.logger.debug("Ricevuto testo libero, lo interpreto come aggiunta spesa");
        return this._aggiungiSpesa(payload.message.text);
      }

      return this.handleCommand(payload.message.text);
    }
    this.logger.debug("Payload Telegram ignorato: nessun testo presente");
    return { success: null, error: "Payload non valido" };
  }

  handleCommand(text) {
    const parts = text.split(" ");
    const command = parts[0];
    const args = text.substring(command.length).trim();

    const commandFunc = this.commands[command];

    if (commandFunc) {
      this.logger.debug(`Esecuzione comando Telegram riconosciuto: ${command}`);
      return commandFunc(args);
    } else {
      this.logger.debug(`Comando Telegram non riconosciuto: ${command}`);
      return { success: null, error: "Comando non riconosciuto. Usa /totale o /cancella, oppure scrivi direttamente la spesa per aggiungerla." };
    }
  }

  _aggiungiSpesa(text) {
    if (!text || text.trim() === "") {
      return { success: null, error: "Testo della spesa non valido." };
    }
    try {
      const extractedData = this.gemini.extractExpenseData(text);
      if (extractedData.error) {
        return { success: null, error: `<b>Errore di elaborazione:</b>\n${extractedData.error}` };
      }

      const dataOggi = Utilities.formatDate(new Date(), "GMT+02:00", "dd/MM/yyyy");
      this.db.appendExpense(dataOggi, extractedData.negozio, extractedData.importo);

      return { success: `<b>Nuova Spesa Salvata!</b>\nNegozio: ${extractedData.negozio}\nImporto: ${extractedData.importo} €`, error: null };
    } catch (e) {
      return { success: null, error: `Errore imprevisto:\n${e.message}` };
    }
  }

  _calcolaTotaleMese() {
    const data = this.db.getExpenses();
    const meseCorrente = new Date().getMonth();
    let totale = 0;

    for (let i = 1; i < data.length; i++) {
      let dataRiga = data[i][0];
      let importo = parseFloat(data[i][2].toString().replace(",", "."));

      let isCurrentMonth = false;
      if (dataRiga instanceof Date) {
        isCurrentMonth = dataRiga.getMonth() === meseCorrente;
      } else if (typeof dataRiga === 'string') {
        const parts = dataRiga.split('/');
        if (parts.length === 3) {
          const month = parseInt(parts[1], 10) - 1;
          isCurrentMonth = month === meseCorrente;
        }
      }

      if (isCurrentMonth && !isNaN(importo)) {
        totale += importo;
      }
    }
    return { success: `📊 <b>Totale speso questo mese:</b> ${totale.toFixed(2)} €`, error: null };
  }

  _eliminaUltimaRiga() {
    const datiCancellati = this.db.deleteLastExpense();
    if (datiCancellati) {
      let dataFormattata = datiCancellati[0];
      if (dataFormattata instanceof Date) {
        dataFormattata = Utilities.formatDate(dataFormattata, "GMT+02:00", "dd/MM/yyyy");
      }
      return { success: `🗑️ <b>Riga eliminata!</b>\nData: ${dataFormattata}\nNegozio: ${datiCancellati[1]}\nImporto: ${datiCancellati[2]}`, error: null };
    } else {
      return { success: null, error: "Il foglio è vuoto, nulla da cancellare." };
    }
  }
}