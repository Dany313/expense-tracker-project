require('dotenv').config();
const express = require('express');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const app = express();
app.use(express.json());

// 1. MOCK DELLE CLASSI E SERVIZI DI GOOGLE (Polyfill)
global.PropertiesService = {
  getScriptProperties: () => ({
    getProperty: (key) => process.env[key]
  })
};

global.UrlFetchApp = {
  fetch: async (url, options) => {
    console.log(`[Mock UrlFetchApp] Chiamata a: ${url}`);
    
    // Convertiamo eventuali payload Google Apps Script in formato node fetch
    let fetchOptions = { ...options };
    if (fetchOptions.payload) {
      fetchOptions.body = fetchOptions.payload;
      delete fetchOptions.payload;
    }
    if (fetchOptions.contentType) {
      fetchOptions.headers = {
        ...fetchOptions.headers,
        'Content-Type': fetchOptions.contentType
      };
      delete fetchOptions.contentType;
    }

    try {
      const res = await fetch(url, fetchOptions);
      const text = await res.text();
      return {
        getContentText: () => text,
        getResponseCode: () => res.status
      };
    } catch (e) {
      console.error("[Mock UrlFetchApp] Errore fetch:", e);
      throw e;
    }
  }
};

global.HtmlService = {
  createHtmlOutput: (text) => ({ type: 'html', content: text })
};

global.ContentService = {
  MimeType: { TEXT: 'text/plain', JSON: 'application/json' },
  createTextOutput: (text) => {
    return {
      type: 'text',
      content: text,
      setMimeType: function(mime) {
        this.mime = mime;
        return this;
      }
    };
  }
};

global.Utilities = {
  formatDate: (date, timezone, format) => {
    // Semplice mock di formattazione data per "dd/MM/yyyy"
    const d = new Date(date);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  }
};

// Carica tutti i file .js della cartella nel contesto globale (simulando l'ambiente Google Apps Script)
const loadGASFiles = () => {
  const files = ['Constants.js', 'Logger.js', 'GeminiClient.js', 'TelegramClient.js', 'main.js'];
  for (const file of files) {
    if (fs.existsSync(path.join(__dirname, file))) {
      const code = fs.readFileSync(path.join(__dirname, file), 'utf8');
      vm.runInThisContext(code, file);
    }
  }
};

loadGASFiles();

// 2. ENDPOINT LOCALE PER POSTMAN
app.post('/exec', (req, res) => {
  console.log("📥 Ricevuto payload da Postman:", req.body);
  
  // Costruiamo l'oggetto "e" simulando la struttura di Google (doPost)
  const mockEvent = {
    postData: { contents: JSON.stringify(req.body) },
    parameter: req.query
  };

  try {
    // Chiama la funzione doPost definita in main.js
    if (typeof doPost === 'function') {
      const result = doPost(mockEvent);
      
      // Mappiamo la risposta per Express
      if (result && result.mime) {
        res.set('Content-Type', result.mime);
      }
      res.status(200).send(result ? result.content : '');
    } else {
      res.status(500).send("Funzione doPost non trovata. Assicurati che main.js contenga doPost(e).");
    }
  } catch (error) {
    console.error("Errore nell'esecuzione di doPost:", error);
    res.status(500).send(error.toString());
  }
});

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`🚀 Server locale attivo su http://localhost:${port}/exec`));
