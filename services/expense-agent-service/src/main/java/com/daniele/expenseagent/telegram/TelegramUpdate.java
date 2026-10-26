package com.daniele.expenseagent.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(
        @JsonProperty("update_id") Long updateId,
        @JsonProperty("message") TelegramMessage message
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramMessage(
            @JsonProperty("message_id") Long messageId,
            @JsonProperty("from") TelegramUser from,
            @JsonProperty("chat") TelegramChat chat,
            @JsonProperty("text") String text,
            @JsonProperty("caption") String caption,
            @JsonProperty("voice") TelegramVoice voice,
            @JsonProperty("photo") List<TelegramPhotoSize> photo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramUser(
            @JsonProperty("id") Long id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("username") String username
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramChat(
            @JsonProperty("id") Long id,
            @JsonProperty("type") String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramVoice(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("mime_type") String mimeType,
            @JsonProperty("duration") Integer duration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramPhotoSize(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("width") Integer width,
            @JsonProperty("height") Integer height,
            @JsonProperty("file_size") Integer fileSize
    ) {
    }
}
