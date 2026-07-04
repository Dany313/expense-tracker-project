class TelegramClient extends IClientHandler {
  constructor() {
    super();
    this.token = PropertiesService.getScriptProperties().getProperty("TELEGRAM_TOKEN");
    this.chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID");
    this.apiUrl = `https://api.telegram.org/bot${this.token}`;
  }

  // --- IMPLEMENTAZIONE INTERFACCIA ---
  handleRequest(payload) {
    if (payload.message && payload.message.text) {
      if (payload.message.chat.id.toString() !== this.chatId) {
         return HtmlService.createHtmlOutput(""); 
      }
      this.handleCommand(payload.message.text);
    }
    // Risposta 200 OK anti-loop
    return HtmlService.createHtmlOutput("");
  }

  // Funzione per inviare un messaggio testuale
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

  // Funzione che smista i comandi ricevuti dal bot
  handleCommand(text) {
    const sheetRepo = new Logger(); // Riutilizziamo la tua classe Logger/Repository
    const sheet = sheetRepo.sheet;

    if (text === "/totale") {
      this._calcolaTotaleMese(sheet);
    } 
    else if (text === "/cancella") {
      this._eliminaUltimaRiga(sheet);
    }
    else if (text === "/log") {
      this.sendMessage("I log completi sono visualizzabili solo su Google Sheets per motivi di spazio.");
    }
    else {
      this.sendMessage("Comando non riconosciuto. Usa /totale, /cancella o /log.");
    }
  }

  _calcolaTotaleMese(sheet) {
    const data = sheet.getDataRange().getValues();
    const meseCorrente = new Date().getMonth();
    let totale = 0;

    for (let i = 1; i < data.length; i++) { // Salta l'intestazione
      let dataRiga = data[i][0]; // Assumendo che la data sia in colonna A
      let importo = parseFloat(data[i][2].toString().replace(",", ".")); // Colonna C
      
      // Controllo grezzo sul mese (migliorabile)
      if (dataRiga instanceof Date && dataRiga.getMonth() === meseCorrente) {
        if (!isNaN(importo)) totale += importo;
      }
    }
    this.sendMessage(`📊 <b>Totale speso questo mese:</b> ${totale.toFixed(2)} €`);
  }

  _eliminaUltimaRiga(sheet) {
    const ultimaRiga = sheet.getLastRow();
    if (ultimaRiga > 1) { // Evita di cancellare l'intestazione
      const datiCancellati = sheet.getRange(ultimaRiga, 1, 1, 3).getValues()[0];
      sheet.deleteRow(ultimaRiga);
      this.sendMessage(`🗑️ <b>Riga eliminata!</b>\nData: ${Utilities.formatDate(datiCancellati[0], "GMT+02:00", "dd/MM/yyyy")}\nNegozio: ${datiCancellati[1]}\nImporto: ${datiCancellati[2]}`);
    } else {
      this.sendMessage("Il foglio è vuoto, nulla da cancellare.");
    }
  }
}