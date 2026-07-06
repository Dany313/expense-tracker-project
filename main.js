function doPost(e) {
  const db = new SpreadSheetService();
  const telegram = new TelegramClient(db);
  const logger = new Logger(db, telegram);
  const gemini = new GeminiClient();
  const macrodroid = new MacroDroidClient(gemini, logger, db);

  const gateway = new GatewayClient(telegram, macrodroid, logger);
  return gateway.processRequest(e);
}