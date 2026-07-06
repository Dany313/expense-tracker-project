describe('Logger', () => {
  let dbMock;

  beforeEach(() => {
    dbMock = { appendLog: jest.fn() };
    global.UrlFetchApp = { fetch: jest.fn() };
  });

  afterEach(() => {
    delete global.UrlFetchApp;
  });

  test('should log INFO to database only', () => {
    const logger = new Logger(dbMock);
    logger.info("Test Info");

    expect(dbMock.appendLog).toHaveBeenCalledWith("04/07/2026", "INFO", "Test Info");
    expect(global.UrlFetchApp.fetch).not.toHaveBeenCalled();
  });

  test('should send SUCCESS to Telegram if bot token present', () => {
    const logger = new Logger(dbMock);
    logger.success("Test Success");

    expect(dbMock.appendLog).toHaveBeenCalledWith("04/07/2026", "SUCCESS", "Test Success");
    expect(global.UrlFetchApp.fetch).toHaveBeenCalled();
    const fetchArgs = global.UrlFetchApp.fetch.mock.calls[0];
    expect(fetchArgs[0]).toContain("bottest-bot-token");
    const payload = JSON.parse(fetchArgs[1].payload);
    expect(payload.text).toContain("✅");
  });

  test('should send ERROR to Telegram if bot token present', () => {
    const logger = new Logger(dbMock);
    logger.error("Test Error");

    expect(dbMock.appendLog).toHaveBeenCalledWith("04/07/2026", "ERROR", "Test Error");
    expect(global.UrlFetchApp.fetch).toHaveBeenCalled();
    const fetchArgs = global.UrlFetchApp.fetch.mock.calls[0];
    const payload = JSON.parse(fetchArgs[1].payload);
    expect(payload.text).toContain("🔴");
  });
});
