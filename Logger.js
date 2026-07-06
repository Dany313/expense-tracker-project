class Logger {
  constructor(spreadSheetService) {
    this.db = spreadSheetService;
    this.token = PropertiesService.getScriptProperties().getProperty("TELEGRAM_BOT_TOKEN") || PropertiesService.getScriptProperties().getProperty("TELEGRAM_TOKEN");
    this.chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID");
    if (this.token) {
      this.apiUrl = `https://api.telegram.org/bot${this.token}`;
    }
  }

  _log(level, message) {
    const timestamp = Utilities.formatDate(new Date(), "GMT+02:00", "dd/MM/yyyy HH:mm:ss");
    const logMessage = typeof message === 'object' ? JSON.stringify(message) : message;

    if (this.db) {
      this.db.appendLog(timestamp, level, logMessage);
    }

    if (this.apiUrl && this.chatId && (level === "SUCCESS" || level === "ERROR")) {
      const emoji = level === "ERROR" ? "🔴" : "✅";

      const payload = {
        chat_id: this.chatId,
        text: `${emoji} ${logMessage}`,
        parse_mode: "HTML"
      };

      UrlFetchApp.fetch(`${this.apiUrl}/sendMessage`, {
        method: "post",
        contentType: "application/json",
        payload: JSON.stringify(payload),
        muteHttpExceptions: true
      });
    }
  }

  info(message) { this._log("INFO", message); }
  debug(message) { this._log("DEBUG", message); }
  success(message) { this._log("SUCCESS", message); }
  error(message) { this._log("ERROR", message); }
}