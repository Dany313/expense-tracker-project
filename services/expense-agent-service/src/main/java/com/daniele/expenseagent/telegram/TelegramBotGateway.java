package com.daniele.expenseagent.telegram;

public interface TelegramBotGateway {

    byte[] downloadTelegramFile(String fileId);

    void sendChatMessage(Long chatId, String text);

    String enqueueReceiptPhotoOcr(String base64Image, String mimeType);
}
