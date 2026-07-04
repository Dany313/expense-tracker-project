class IClientHandler {
  handleRequest(payload) {
    throw new Error("Il metodo handleRequest(payload) deve essere implementato dalla classe figlia.");
  }
}
