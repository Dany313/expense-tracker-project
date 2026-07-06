function doPost(e) {
  const db = new SpreadSheetService();
  const logger = new Logger(db);

  const gemini = new GeminiClient(logger);
  const telegram = new TelegramClient(db, gemini, logger);
  const macrodroid = new MacroDroidClient(gemini, db, logger);

  const gateway = new GatewayClient(telegram, macrodroid, logger);
  return gateway.processRequest(e);
}