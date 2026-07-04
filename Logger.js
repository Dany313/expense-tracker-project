class Logger {
  constructor() {
    this.sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.MAIN_TAB);
    this.logSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.LOG_TAB);
  }

  saveExpense(date, store, amount) {
    if (!this.sheet) throw new Error("Foglio dati non trovato");
    this.sheet.appendRow([date, store, amount]);
  }

  log(level, message) {
    if (!this.logSheet) return; // Fallback silenzioso
    const timestamp = Utilities.formatDate(new Date(), "GMT+02:00", "dd/MM/yyyy HH:mm:ss");
    this.logSheet.appendRow([timestamp, level, message]);
  }
}