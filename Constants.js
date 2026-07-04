class Constants {

    static get MAPPA_NEGOZI() {
    return {
      "ALDI": ["ALDI", "ALDI SUISSE"],
      "CONAD": ["CONAD", "SPAZIO CONAD", "MARGHERITA", "CONAD SUPERSTORE"],
      "FAMILA": ["FAMILA", "MAXI FAMILA", "MEGAMARK"],
      "EUROSPIN": ["EUROSPIN", "EUROSPIN ITALIA"],
      "TIGOTA": ["TIGOTA", "TIGOTÀ", "GOTTARDO"],
      "ESSELUNGA": ["ESSELUNGA"],
      "HAPPYCASA": ["HAPPYCASA", "HAPPY CASA"]
    };
  }

  static get MAIN_TAB(){
    return "scontrini"
  }

  static get LOG_TAB(){
    return "Log Sistema"
  }

  static get MODEL_NAME() {
    return "gemini-3.1-flash-lite"
  }

}
