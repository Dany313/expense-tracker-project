describe('MacroDroidClient', () => {
  let geminiMock, loggerMock, dbMock;

  beforeEach(() => {
    geminiMock = {
      extractExpenseData: jest.fn().mockReturnValue({ negozio: 'TEST_STORE', importo: '25,50', error: null })
    };
    loggerMock = { log: jest.fn() };
    dbMock = { appendExpense: jest.fn() };
  });

  test('should process macrodroid payload and save expense', () => {
    const client = new MacroDroidClient(geminiMock, loggerMock, dbMock);
    const result = client.handleRequest({ testo: 'Hai pagato 25,50 da TEST_STORE' });

    expect(geminiMock.extractExpenseData).toHaveBeenCalledWith('Hai pagato 25,50 da TEST_STORE');
    expect(dbMock.appendExpense).toHaveBeenCalledWith("04/07/2026", "TEST_STORE", "25,50");
    expect(loggerMock.log).toHaveBeenCalledWith("SUCCESS", expect.stringContaining("✅"));
    expect(result).toBe("TextOutput: Successo: TEST_STORE");
  });

  test('should handle gemini errors and log them', () => {
    const client = new MacroDroidClient(geminiMock, loggerMock, dbMock);
    geminiMock.extractExpenseData.mockReturnValue({ negozio: null, importo: null, error: 'non posso eseguire la richiesta' });

    const result = client.handleRequest({ testo: 'Notifica senza senso' });

    expect(geminiMock.extractExpenseData).toHaveBeenCalledWith('Notifica senza senso');
    expect(loggerMock.log).toHaveBeenCalledWith("ERROR", expect.stringContaining('non posso eseguire la richiesta'));
    expect(dbMock.appendExpense).not.toHaveBeenCalled();
    expect(result).toBe("TextOutput: Errore: non posso eseguire la richiesta");
  });

  test('should throw if testo is missing', () => {
    const client = new MacroDroidClient(geminiMock, loggerMock, dbMock);
    expect(() => client.handleRequest({})).toThrow("Payload MacroDroid senza campo 'testo'");
  });
});
