describe('GatewayClient', () => {
  let mockEvent;
  let mockTelegram;
  let mockMacroDroid;
  let mockLogger;
  
  beforeEach(() => {
    mockTelegram = { handleRequest: jest.fn(() => "TelegramResult") };
    mockMacroDroid = { handleRequest: jest.fn(() => "MacroDroidResult") };
    mockLogger = { log: jest.fn() };

    mockEvent = {
      postData: {
        contents: JSON.stringify({ client: 'macrodroid', testo: 'test' })
      }
    };
  });

  test('should return HtmlOutput empty string on error (invalid payload)', () => {
    const gateway = new GatewayClient(mockTelegram, mockMacroDroid, mockLogger);
    const result = gateway.processRequest({});
    expect(result).toBe("HtmlOutput");
    expect(mockLogger.log).toHaveBeenCalledWith("ERROR", expect.any(String));
  });

  test('should route to MacroDroidClient', () => {
    const gateway = new GatewayClient(mockTelegram, mockMacroDroid, mockLogger);
    const result = gateway.processRequest(mockEvent);
    expect(mockMacroDroid.handleRequest).toHaveBeenCalled();
    expect(result).toBe("MacroDroidResult");
  });

  test('should route to TelegramClient via update_id', () => {
    mockEvent.postData.contents = JSON.stringify({ update_id: 1234 });
    const gateway = new GatewayClient(mockTelegram, mockMacroDroid, mockLogger);
    const result = gateway.processRequest(mockEvent);
    expect(mockTelegram.handleRequest).toHaveBeenCalled();
    expect(result).toBe("TelegramResult");
  });
});
