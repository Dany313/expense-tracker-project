const fs = require('fs');
const path = require('path');

// --- 1. MOCK GOOGLE APPS SCRIPT GLOBALS ---

global.PropertiesService = {
  getScriptProperties: jest.fn(() => ({
    getProperty: jest.fn((key) => {
      if (key === 'MODEL_API_KEY') return 'test-gemini-key';
      if (key === 'TELEGRAM_TOKEN') return 'test-bot-token';
      if (key === 'TELEGRAM_CHAT_ID') return '123456789';
      return null;
    })
  }))
};

const mockMainSheet = {
  appendRow: jest.fn(),
  getDataRange: jest.fn(() => ({
    getValues: jest.fn(() => [
      ["Data", "Negozio", "Importo"],
      [new Date(), "CONAD", 15.5]
    ])
  })),
  getLastRow: jest.fn(() => 2),
  getRange: jest.fn(() => ({
    getValues: jest.fn(() => [ [new Date(), "CONAD", 15.5] ])
  })),
  deleteRow: jest.fn()
};

const mockLogSheet = {
  appendRow: jest.fn()
};

global.SpreadsheetApp = {
  getActiveSpreadsheet: jest.fn(() => ({
    getSheetByName: jest.fn((name) => {
      if (name === 'scontrini') return mockMainSheet;
      if (name === 'Log Sistema') return mockLogSheet;
      return null;
    })
  }))
};

global.UrlFetchApp = {
  fetch: jest.fn(() => ({
    getResponseCode: jest.fn(() => 200),
    getContentText: jest.fn(() => JSON.stringify({
      candidates: [{
        content: {
          parts: [{ text: '```json\n{"importo": "15,50", "negozio": "CONAD"}\n```' }]
        }
      }]
    }))
  }))
};

global.Utilities = {
  formatDate: jest.fn(() => "04/07/2026")
};

global.HtmlService = {
  createHtmlOutput: jest.fn(() => "HtmlOutput")
};

global.ContentService = {
  MimeType: { TEXT: 'TEXT' },
  createTextOutput: jest.fn((text) => ({
    setMimeType: jest.fn(() => `TextOutput: ${text}`)
  }))
};


// --- 2. LOAD GAS SOURCE FILES INTO GLOBAL CONTEXT ---
// --- 2. LOAD GAS SOURCE FILES INTO GLOBAL CONTEXT ---

const files = [
  'Constants.js',
  'SpreadSheetService.js',
  'Logger.js',
  'GeminiClient.js',
  'TelegramClient.js',
  'MacroDroidClient.js',
  'GatewayClient.js'
];

let combinedCode = '';
for (const file of files) {
  combinedCode += fs.readFileSync(path.join(__dirname, '..', file), 'utf8') + '\n\n';
}

const scriptCode = `
  ${combinedCode}
  global.Constants = Constants;
  global.SpreadSheetService = SpreadSheetService;
  global.Logger = Logger;
  global.GeminiClient = GeminiClient;
  global.TelegramClient = TelegramClient;
  global.MacroDroidClient = MacroDroidClient;
  global.GatewayClient = GatewayClient;
`;

eval(scriptCode);

