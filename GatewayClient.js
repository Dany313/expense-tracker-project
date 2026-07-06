class GatewayClient {
  constructor(telegramClient, macroDroidClient, logger) {
    this.telegram = telegramClient;
    this.macrodroid = macroDroidClient;
    this.logger = logger;
  }

  processRequest(e) {
    this.logger.log("INFO", "NUOVA CONNESSIONE GATEWAY");

    try {
      if (!e || !e.postData || !e.postData.contents) {
        throw new Error("Payload HTTP assente o malformato");
      }

      const payload = JSON.parse(e.postData.contents);

      let clientId = (e.parameter && e.parameter.client) || payload.client;
      if (!clientId && payload.update_id) clientId = "telegram";

      if (clientId === "macrodroid") {
        return this.macrodroid.handleRequest(payload);
      } else if (clientId === "telegram") {
        return this.telegram.handleRequest(payload);
      } else {
        throw new Error(`Client non riconosciuto: ${clientId}`);
      }

    } catch (error) {
      this.logger.log("ERROR", `Gateway Error: ${error.toString()}`);
      return HtmlService.createHtmlOutput("");
    }
  }
}
