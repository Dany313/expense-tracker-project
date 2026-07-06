class GatewayClient {
  constructor(telegramClient, macrodroidClient, logger) {
    this.telegram = telegramClient;
    this.macrodroid = macrodroidClient;
    this.logger = logger;
  }

  processRequest(e) {
    this.logger.info("NUOVA CONNESSIONE GATEWAY");

    try {
      if (!e || !e.postData || !e.postData.contents) {
        throw new Error("Payload HTTP assente o malformato");
      }

      const payload = JSON.parse(e.postData.contents);

      let clientId = (e.parameter && e.parameter.client) || payload.client;
      if (!clientId && payload.update_id) clientId = "telegram";

      let result;
      if (clientId === "macrodroid") {
        result = this.macrodroid.handleRequest(payload);
      } else if (clientId === "telegram") {
        result = this.telegram.handleRequest(payload);
      } else {
        throw new Error(`Client non riconosciuto: ${clientId}`);
      }

      if (result.error) {
        this.logger.error(result.error);
      } else if (result.success) {
        this.logger.success(result.success);
      }

      if (clientId === "macrodroid") {
        return ContentService.createTextOutput(result.error ? `Errore: ${result.error}` : `Successo: ${result.success}`).setMimeType(ContentService.MimeType.TEXT);
      } else {
        return HtmlService.createHtmlOutput("");
      }

    } catch (error) {
      this.logger.error(`Gateway Error: ${error.toString()}`);
      return HtmlService.createHtmlOutput("");
    }
  }
}
