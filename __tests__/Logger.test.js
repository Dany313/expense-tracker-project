describe('Logger', () => {
  let dbMock, telegramMock;

  beforeEach(() => {
    dbMock = { appendLog: jest.fn() };
    telegramMock = { sendMessage: jest.fn() };
  });

  test('should save log to SpreadSheetService', () => {
    const logger = new Logger(dbMock, telegramMock);
    logger.log('DEBUG', 'Test Message');
    
    expect(dbMock.appendLog).toHaveBeenCalledWith("04/07/2026", "DEBUG", "Test Message");
    expect(telegramMock.sendMessage).not.toHaveBeenCalled(); // DEBUG non inoltra a telegram
  });

  test('should forward INFO and ERROR logs to Telegram', () => {
    const logger = new Logger(dbMock, telegramMock);
    
    logger.log('INFO', 'Test Info');
    expect(telegramMock.sendMessage).toHaveBeenCalledWith(expect.stringContaining('[INFO]'));

    logger.log('ERROR', 'Test Error');
    expect(telegramMock.sendMessage).toHaveBeenCalledWith(expect.stringContaining('[ERROR]'));
  });
});
