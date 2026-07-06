describe('MacroDroidClient', () => {
  let geminiMock, telegramMock, loggerMock, dbMock;

  beforeEach(() => {
    geminiMock = {
      extractExpenseData: jest.fn().mockReturnValue({ negozio: 'TEST_STORE', importo: '25,50' })
    };
    telegramMock = { sendMessage: jest.fn() };
    loggerMock = { log: jest.fn() };
    dbMock = { appendExpense: jest.fn() };
  });

  test('should process macrodroid payload and save expense', () => {
    const client = new MacroDroidClient(geminiMock, telegramMock, loggerMock, dbMock);
    const result = client.handleRequest({ testo: 'Hai pagato 25,50 da TEST_STORE' });

    expect(geminiMock.extractExpenseData).toHaveBeenCalledWith('Hai pagato 25,50 da TEST_STORE');
    expect(dbMock.appendExpense).toHaveBeenCalledWith("04/07/2026", "TEST_STORE", "25,50");
    expect(telegramMock.sendMessage).toHaveBeenCalled();
    expect(result).toBe("TextOutput: Successo: TEST_STORE");
  });

  test('should throw if testo is missing', () => {
    const client = new MacroDroidClient(geminiMock, telegramMock, loggerMock, dbMock);
    expect(() => client.handleRequest({})).toThrow("Payload MacroDroid senza campo 'testo'");
  });
});
