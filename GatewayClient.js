class GatewayClient {
  constructor(requestEvent) {
    this.e = requestEvent;
    this.logger = new Logger();

    // REGISTRY DELLE STRATEGIE
    // Mappa la stringa del client direttamente alla Classe (non all'istanza)
    this.routes = {
      "macrodroid": MacroDroidClient,
      "telegram": TelegramClient
      // Domani basterà aggiungere qui: "plaid": PlaidClient
    };
  }

  processRequest() {
    this.logger.log("INFO", "NUOVA CONNESSIONE GATEWAY");

    try {
      if (!this.e || !this.e.postData || !this.e.postData.contents) {
        throw new Error("Payload HTTP assente o malformato");
      }

      const payload = JSON.parse(this.e.postData.contents);

      // 1. Identifichiamo il client
      // L'approccio migliore è leggerlo dall'URL (?client=...), ma per retrocompatibilità 
      // manteniamo anche il fallback sul body (payload.client) o sulla struttura Telegram (update_id)
      let clientId = (this.e.parameter && this.e.parameter.client) || payload.client;
      if (!clientId && payload.update_id) clientId = "telegram";

      // 2. Recuperiamo la Classe dal Registry (O(1) lookup temporale)
      const HandlerClass = this.routes[clientId];

      if (!HandlerClass) {
        throw new Error(`Client non riconosciuto o non registrato: ${clientId}`);
      }

      // 3. Istanziamo ed eseguiamo l'handler polimorfico
      const handlerClient = new HandlerClass();
      return handlerClient.handleRequest(payload);

    } catch (error) {
      this.logger.log("ERROR", `Gateway Error: ${error.toString()}`);

      const telegram = new TelegramClient();
      telegram.sendMessage(`⚠️ <b>Errore Gateway:</b>\n${error.toString()}`);

      return HtmlService.createHtmlOutput("");
    }
  }
}
