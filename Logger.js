class Logger {
  constructor(spreadSheetService, telegramClient = null) {
    this.db = spreadSheetService;
    this.telegram = telegramClient;
  }

  log(level, message) {
    const timestamp = Utilities.formatDate(new Date(), "GMT+02:00", "dd/MM/yyyy HH:mm:ss");
    const logMessage = typeof message === 'object' ? JSON.stringify(message) : message;

    if (this.db) {
      this.db.appendLog(timestamp, level, logMessage);
    }

    if (this.telegram && (level === "SUCCESS" || level === "ERROR")) {
      const emoji = level === "ERROR" ? "🔴" : "✅";
      this.telegram.sendMessage(`${emoji} ${logMessage}`);
    }
  }
}