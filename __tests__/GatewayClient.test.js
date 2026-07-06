describe('GatewayClient', () => {
  let telegramMock, macrodroidMock, loggerMock;

  beforeEach(() => {
    telegramMock = { handleRequest: jest.fn() };
    macrodroidMock = { handleRequest: jest.fn() };
    loggerMock = { info: jest.fn(), error: jest.fn(), success: jest.fn() };
  });

  test('should route to Telegram and log success', () => {
    telegramMock.handleRequest.mockReturnValue({ success: "ok telegram", error: null });
    const gateway = new GatewayClient(telegramMock, macrodroidMock, loggerMock);

    const event = { postData: { contents: JSON.stringify({ update_id: 123, message: { text: "hi" } }) } };
    const res = gateway.processRequest(event);

    expect(telegramMock.handleRequest).toHaveBeenCalled();
    expect(loggerMock.success).toHaveBeenCalledWith("ok telegram");
    // Returns HtmlService for Telegram
    expect(res).toBe("HtmlOutput");
  });

  test('should route to MacroDroid and log error', () => {
    macrodroidMock.handleRequest.mockReturnValue({ success: null, error: "ops error" });
    const gateway = new GatewayClient(telegramMock, macrodroidMock, loggerMock);

    const event = { postData: { contents: JSON.stringify({ client: "macrodroid", testo: "hi" }) } };
    const res = gateway.processRequest(event);

    expect(macrodroidMock.handleRequest).toHaveBeenCalled();
    expect(loggerMock.error).toHaveBeenCalledWith("ops error");
    expect(res).toBe("TextOutput: Errore: ops error");
  });

  test('should handle completely broken requests', () => {
    const gateway = new GatewayClient(telegramMock, macrodroidMock, loggerMock);
    const res = gateway.processRequest(null);
    expect(loggerMock.error).toHaveBeenCalledWith(expect.stringContaining("assente o malformato"));
  });
});
