describe('Logger', () => {
  test('should save expense to main sheet', () => {
    const logger = new Logger();
    const sheetMock = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.MAIN_TAB);
    
    logger.saveExpense('04/07/2026', 'CONAD', '15,50');
    
    expect(sheetMock.appendRow).toHaveBeenCalledWith(['04/07/2026', 'CONAD', '15,50']);
  });

  test('should save log to log sheet', () => {
    const logger = new Logger();
    const logSheetMock = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.LOG_TAB);
    
    logger.log('INFO', 'Test Message');
    
    expect(logSheetMock.appendRow).toHaveBeenCalled();
    const callArg = logSheetMock.appendRow.mock.calls[0][0];
    expect(callArg[1]).toBe('INFO');
    expect(callArg[2]).toBe('Test Message');
  });
});
