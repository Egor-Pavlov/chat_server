package model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Message {
    private String username;
    private String text;
    private ZonedDateTime timestamp;

    public Message(String username, String text, ZonedDateTime timestamp) {
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.text = Objects.requireNonNull(text, "Text cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    public String getUsername() {
        return username;
    }

    public String getText() {
        return text;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String toJson() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        return String.format("{\"username\":\"%s\",\"text\":\"%s\",\"timestamp\":\"%s\"}",
                username, text, timestamp.format(formatter));
    }

    public static Message fromJson(String json) {
        try {
            String username = json.split("\"username\":\"")[1].split("\"")[0];
            String text = json.split("\"text\":\"")[1].split("\"")[0];
            String timestampStr = json.split("\"timestamp\":\"")[1].split("\"")[0];
            ZonedDateTime timestamp = ZonedDateTime.parse(timestampStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return new Message(username, text, timestamp);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
    }
}
