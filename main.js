function doPost(e) {
  // Istanziamo il Gateway passando la request cruda
  const gateway = new GatewayClient(e);
  
  // Eseguiamo il routing e restituiamo direttamente l'output HTTP corretto
  return gateway.processRequest();
}