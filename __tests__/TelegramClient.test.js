describe('TelegramClient', () => {
  test('should handle authorized message commands', () => {
    const client = new TelegramClient();
    
    const spy = jest.spyOn(client, 'handleCommand').mockImplementation(() => {});
    
    client.handleRequest({
      message: {
        chat: { id: 123456789 },
        text: '/totale'
      }
    });

    expect(spy).toHaveBeenCalledWith('/totale');
    spy.mockRestore();
  });

  test('should reject unauthorized users', () => {
    const client = new TelegramClient();
    
    const spy = jest.spyOn(client, 'handleCommand').mockImplementation(() => {});
    
    client.handleRequest({
      message: {
        chat: { id: 999999 }, // Different ID
        text: '/totale'
      }
    });

    expect(spy).not.toHaveBeenCalled();
    spy.mockRestore();
  });
});
