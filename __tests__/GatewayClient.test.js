describe('GatewayClient', () => {
  let mockEvent;
  
  beforeEach(() => {
    mockEvent = {
      postData: {
        contents: JSON.stringify({ client: 'macrodroid', testo: 'test' })
      }
    };
  });

  test('should return HtmlOutput empty string on error (invalid payload)', () => {
    const gateway = new GatewayClient({});
    const result = gateway.processRequest();
    expect(result).toBe("HtmlOutput"); // mock
  });

  test('should route to MacroDroidClient', () => {
    const gateway = new GatewayClient(mockEvent);
    // Spy on MacroDroidClient handleRequest
    const spy = jest.spyOn(MacroDroidClient.prototype, 'handleRequest').mockImplementation(() => "MacroDroidResult");
    
    const result = gateway.processRequest();
    expect(spy).toHaveBeenCalled();
    expect(result).toBe("MacroDroidResult");
    
    spy.mockRestore();
  });

  test('should route to TelegramClient via update_id', () => {
    mockEvent.postData.contents = JSON.stringify({ update_id: 1234 });
    const gateway = new GatewayClient(mockEvent);
    
    const spy = jest.spyOn(TelegramClient.prototype, 'handleRequest').mockImplementation(() => "TelegramResult");
    
    const result = gateway.processRequest();
    expect(spy).toHaveBeenCalled();
    expect(result).toBe("TelegramResult");
    
    spy.mockRestore();
  });
});
