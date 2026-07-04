describe('MacroDroidClient', () => {
  test('should process macrodroid payload and save expense', () => {
    const client = new MacroDroidClient();
    
    const geminiSpy = jest.spyOn(GeminiClient.prototype, 'extractExpenseData').mockReturnValue({
      negozio: 'TEST_STORE',
      importo: '25,50'
    });
    
    const loggerSpy = jest.spyOn(Logger.prototype, 'saveExpense').mockImplementation(() => {});
    const telegramSpy = jest.spyOn(TelegramClient.prototype, 'sendMessage').mockImplementation(() => {});

    const result = client.handleRequest({ testo: 'Hai pagato 25,50 da TEST_STORE' });

    expect(geminiSpy).toHaveBeenCalledWith('Hai pagato 25,50 da TEST_STORE');
    expect(loggerSpy).toHaveBeenCalledWith("04/07/2026", "TEST_STORE", "25,50");
    expect(telegramSpy).toHaveBeenCalled();
    expect(result).toBe("TextOutput: Successo: TEST_STORE");

    geminiSpy.mockRestore();
    loggerSpy.mockRestore();
    telegramSpy.mockRestore();
  });

  test('should throw if testo is missing', () => {
    const client = new MacroDroidClient();
    expect(() => client.handleRequest({})).toThrow("Payload MacroDroid senza campo 'testo'");
  });
});
