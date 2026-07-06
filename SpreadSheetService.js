class SpreadSheetService {
  constructor() {
    this.isLocal = typeof SpreadsheetApp === 'undefined';
  }

  appendExpense(date, store, amount) {
    if (this.isLocal) {
      this._appendCsv('mock_scontrini.csv', `${date},${store},${amount}`);
    } else {
      const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.MAIN_TAB);
      if (sheet) sheet.appendRow([date, store, amount]);
    }
  }

  appendLog(timestamp, level, message) {
    if (this.isLocal) {
      this._appendCsv('mock_log.csv', `${timestamp},${level},${message}`);
    } else {
      const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.LOG_TAB);
      if (sheet) sheet.appendRow([timestamp, level, message]);
    }
  }

  getExpenses() {
    if (this.isLocal) {
      return this._readCsv('mock_scontrini.csv');
    } else {
      const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.MAIN_TAB);
      if (!sheet) return [];
      return sheet.getDataRange().getValues();
    }
  }

  deleteLastExpense() {
    if (this.isLocal) {
      return this._deleteLastLineCsv('mock_scontrini.csv');
    } else {
      const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(Constants.MAIN_TAB);
      if (!sheet) return null;
      const lastRow = sheet.getLastRow();
      if (lastRow > 1) {
        const deletedData = sheet.getRange(lastRow, 1, 1, 3).getValues()[0];
        sheet.deleteRow(lastRow);
        return deletedData;
      }
      return null;
    }
  }

  // --- METODI MOCK LOCALI (Usati solo da Postman) ---
  _appendCsv(filename, rowString) {
    if (typeof require !== 'undefined') {
      const fs = require('fs');
      const path = require('path');
      const filepath = path.join(process.cwd(), filename);
      fs.appendFileSync(filepath, `\n${rowString}`);
    }
  }

  _readCsv(filename) {
    if (typeof require !== 'undefined') {
      const fs = require('fs');
      const path = require('path');
      const filepath = path.join(process.cwd(), filename);
      if (!fs.existsSync(filepath)) return [];
      const content = fs.readFileSync(filepath, 'utf8');
      return content.trim().split('\n').map(line => line.split(','));
    }
    return [];
  }

  _deleteLastLineCsv(filename) {
    if (typeof require !== 'undefined') {
      const fs = require('fs');
      const path = require('path');
      const filepath = path.join(process.cwd(), filename);
      if (!fs.existsSync(filepath)) return null;
      
      let content = fs.readFileSync(filepath, 'utf8').trim().split('\n');
      if (content.length <= 1) return null; 
      
      const lastLine = content.pop().split(',');
      fs.writeFileSync(filepath, content.join('\n'));
      return lastLine;
    }
    return null;
  }
}
