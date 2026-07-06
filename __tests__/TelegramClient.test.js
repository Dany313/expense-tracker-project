describe('TelegramClient', () => {
  let dbMock, geminiMock, loggerMock;

  beforeEach(() => {
    dbMock = { getExpenses: jest.fn(), deleteLastExpense: jest.fn(), appendExpense: jest.fn() };
    geminiMock = { extractExpenseData: jest.fn() };
    loggerMock = { debug: jest.fn(), info: jest.fn(), error: jest.fn(), success: jest.fn() };
  });

  test('should handle authorized message commands', () => {
    const client = new TelegramClient(dbMock, geminiMock, loggerMock);
    const spy = jest.spyOn(client, 'handleCommand').mockReturnValue({ success: "ok", error: null });
    
    const res = client.handleRequest({
      message: {
        chat: { id: 123456789 },
        text: '/totale'
      }
    });

    expect(spy).toHaveBeenCalledWith('/totale');
    expect(res).toEqual({ success: "ok", error: null });
    spy.mockRestore();
  });

  test('should reject unauthorized users', () => {
    const client = new TelegramClient(dbMock, geminiMock, loggerMock);
    const spy = jest.spyOn(client, 'handleCommand').mockImplementation(() => {});
    
    const res = client.handleRequest({
      message: {
        chat: { id: 999999 },
        text: '/totale'
      }
    });

    expect(spy).not.toHaveBeenCalled();
    expect(res).toEqual({ success: null, error: "Accesso non autorizzato" });
    spy.mockRestore();
  });

  test('should handle free text as expense via Gemini', () => {
    const client = new TelegramClient(dbMock, geminiMock, loggerMock);
    geminiMock.extractExpenseData.mockReturnValue({ importo: '10,00', negozio: 'BAR', error: null });
    
    // Instead of calling handleCommand, we simulate a free text request in handleRequest
    const res = client.handleRequest({
      message: {
        chat: { id: 123456789 },
        text: 'la spesa di 10 euro al bar'
      }
    });

    expect(geminiMock.extractExpenseData).toHaveBeenCalledWith("la spesa di 10 euro al bar");
    expect(dbMock.appendExpense).toHaveBeenCalledWith("04/07/2026", "BAR", "10,00");
    expect(res.success).toContain("Nuova Spesa Salvata");
    expect(res.error).toBeNull();
  });

  test('should reject unknown command', () => {
    const client = new TelegramClient(dbMock, geminiMock, loggerMock);
    const res = client.handleCommand("/sconosciuto 10 euro al bar");
    expect(res.error).toContain("Comando non riconosciuto");
  });
});
