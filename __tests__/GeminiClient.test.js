describe('GeminiClient', () => {
  test('should extract data correctly calling UrlFetchApp', () => {
    const client = new GeminiClient();
    
    const result = client.extractExpenseData("testo notifica");
    expect(result).toEqual({ importo: "15,50", negozio: "CONAD", error: null });
  });

  test('should throw if API Key is missing', () => {
    // Override PropertiesService mock just for this test
    const originalMock = PropertiesService.getScriptProperties;
    PropertiesService.getScriptProperties = jest.fn(() => ({
      getProperty: () => null
    }));
    
    expect(() => new GeminiClient()).toThrow("API Key mancante");
    
    PropertiesService.getScriptProperties = originalMock; // Restore
  });
});
