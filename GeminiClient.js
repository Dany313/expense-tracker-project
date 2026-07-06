class GeminiClient {

  constructor() {
    this.apiKey = PropertiesService.getScriptProperties().getProperty("MODEL_API_KEY");
    this.modelName = Constants.MODEL_NAME;
    
    if (!this.apiKey) throw new Error("API Key mancante");
    this.url = `https://generativelanguage.googleapis.com/v1beta/models/${this.modelName}:generateContent?key=${this.apiKey}`;
  }

  extractExpenseData(notificationText) {
    const prompt = this._buildPrompt(notificationText);
    const payload = { "contents": [{"parts":[{"text": prompt}]}] };
    const options = {
      "method": "post",
      "contentType": "application/json",
      "payload": JSON.stringify(payload),
      "muteHttpExceptions": true 
    };

    const response = UrlFetchApp.fetch(this.url, options);
    if (response.getResponseCode() !== 200) {
       throw new Error(`Errore API Gemini: ${response.getContentText()}`);
    }
    
    return this._parseResponse(response.getContentText());
  }

  _parseResponse(responseText) {
    const jsonResponse = JSON.parse(responseText);
    let rawOutput = jsonResponse.candidates[0].content.parts[0].text;
    rawOutput = rawOutput.replace(/```json/gi, "").replace(/```/g, "").trim();
    return JSON.parse(rawOutput);
  }

  _buildPrompt(text) {
  
  return "Sei un estrattore dati per scontrini finanziari. Analizza questo testo di notifica: '" + text + "'.\n\n" +
               "REGOLE DI ESTRAZIONE:\n" +
               "1. Se la richiesta non è inerente a una spesa o non puoi estrarre i dati, compila il campo 'error' con 'non posso eseguire la richiesta' e metti null agli altri campi.\n" +
               "2. IMPORTO: Trova l'importo pagato e restituiscilo formattato usando solo la virgola come separatore decimale (es. 15,50). Ometti il simbolo dell'euro o di qualsiasi altra valuta.\n" +
               "3. NEGOZIO: Estrai il nome del negozio in cui è avvenuto il pagamento.\n" +
               "4. MAPPATURA: Confronta il nome del negozio estratto con questo dizionario JSON:\n" + 
               JSON.stringify(Constants.MAPPA_NEGOZI) + "\n" +
               "Prova prima a estrarre il nome del negozio dalla notifica anche cercando nel web, se ne sei sicuro al 100% allora inseriscilo nella risposta in maiuscolo (esempi: CONAD , COOP , ALDI , EUROSPIN). Se non riesci ad estrarre il nome del negozio dalla notifica utilizza queste istruzioni: se il nome estratto contiene una delle parole chiave presenti negli array del dizionario, usa la chiave principale corrispondente (es. se trovi 'SPAZIO CONAD', usa 'CONAD').\n" +
               "Se non trovi nessuna corrispondenza logica nel dizionario, usa semplicemente il nome del negozio che hai estratto dal testo, mettendolo in MAIUSCOLO.\n\n" +
               "OUTPUT:\n" +
               "Rispondi ESATTAMENTE e SOLO con un JSON valido in questo formato: {\"importo\": \"...\", \"negozio\": \"...\", \"error\": null}\n" +
               "In caso di errore o se la richiesta non è pertinente, il JSON deve essere: {\"importo\": null, \"negozio\": null, \"error\": \"non posso eseguire la richiesta\"}";
  }
}