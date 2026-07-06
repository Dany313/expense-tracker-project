describe('MacroDroidClient', () => {
  let geminiMock, dbMock, loggerMock;

  beforeEach(() => {
    geminiMock = { extractExpenseData: jest.fn() };
    dbMock = { appendExpense: jest.fn() };
    loggerMock = { debug: jest.fn(), info: jest.fn(), error: jest.fn(), success: jest.fn() };
  });

  test('should process valid request successfully', () => {
    const client = new MacroDroidClient(geminiMock, dbMock, loggerMock);
    geminiMock.extractExpenseData.mockReturnValue({ importo: '5,00', negozio: 'Panificio', error: null });

    const payload = { client: 'macrodroid', testo: 'Speso 5 euro dal panettiere' };
    const res = client.handleRequest(payload);

    expect(dbMock.appendExpense).toHaveBeenCalledWith("04/07/2026", "Panificio", "5,00");
    expect(res.success).toContain("Nuova Spesa Salvata");
    expect(res.error).toBeNull();
  });

  test('should return error if payload misses testo', () => {
    const client = new MacroDroidClient(geminiMock, dbMock, loggerMock);
    const res = client.handleRequest({ client: 'macrodroid' });
    expect(res.error).toContain("senza campo 'testo'");
  });
});
