class TelegramClient {
  constructor(spreadSheetService) {
    this.token = PropertiesService.getScriptProperties().getProperty("TELEGRAM_BOT_TOKEN") || PropertiesService.getScriptProperties().getProperty("TELEGRAM_TOKEN");
    this.chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID");
    this.apiUrl = `https://api.telegram.org/bot${this.token}`;
    this.db = spreadSheetService;

    // Dizionario dei comandi: mappa il testo del comando a una funzione
    this.commands = {
      "/totale": () => this._calcolaTotaleMese(),
      "/cancella": () => this._eliminaUltimaRiga()
    };
  }

  handleRequest(payload) {
    if (payload.message && payload.message.text) {
      if (this.chatId && payload.message.chat.id.toString() !== this.chatId) {
        return HtmlService.createHtmlOutput("");
      }
      this.handleCommand(payload.message.text);
    }
    // Risposta 200 OK anti-loop
    return HtmlService.createHtmlOutput("");
  }

  sendMessage(text) {
    const payload = {
      chat_id: this.chatId,
      text: text,
      parse_mode: "HTML"
    };

    UrlFetchApp.fetch(`${this.apiUrl}/sendMessage`, {
      method: "post",
      contentType: "application/json",
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });
  }

  handleCommand(text) {
    const commandFunc = this.commands[text];

    if (commandFunc) {
      commandFunc();
    } else {
      this.sendMessage("Comando non riconosciuto. Usa /totale, /cancella o /log.");
    }
  }

  _calcolaTotaleMese() {
    const data = this.db.getExpenses();
    const meseCorrente = new Date().getMonth();
    let totale = 0;

    for (let i = 1; i < data.length; i++) { // Salta l'intestazione
      let dataRiga = data[i][0]; // Assumendo che la data sia in colonna A
      let importo = parseFloat(data[i][2].toString().replace(",", ".")); // Colonna C

      // Controllo grezzo sul mese: format data GG/MM/AAAA
      // In JS new Date() prende "MM/DD/YYYY" o ISO, se la dataRiga è stringa GG/MM/AAAA dobbiamo parsare
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
    this.sendMessage(`📊 <b>Totale speso questo mese:</b> ${totale.toFixed(2)} €`);
  }

  _eliminaUltimaRiga() {
    const datiCancellati = this.db.deleteLastExpense();
    if (datiCancellati) {
      let dataFormattata = datiCancellati[0];
      if (dataFormattata instanceof Date) {
        dataFormattata = Utilities.formatDate(dataFormattata, "GMT+02:00", "dd/MM/yyyy");
      }
      this.sendMessage(`🗑️ <b>Riga eliminata!</b>\nData: ${dataFormattata}\nNegozio: ${datiCancellati[1]}\nImporto: ${datiCancellati[2]}`);
    } else {
      this.sendMessage("Il foglio è vuoto, nulla da cancellare.");
    }
  }
}