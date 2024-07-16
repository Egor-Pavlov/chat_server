package model;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private String username;
    private String text;
    private ZonedDateTime timestamp;

    public Message(String username, String text, ZonedDateTime timestamp) {
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String toJson() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        return String.format("{\"username\":\"%s\",\"text\":\"%s\",\"timestamp\":\"%s\"}",
                username, text, timestamp.format(formatter));
    }

    public static Message fromJson(String json) {
        String username = json.split("\"username\":\"")[1].split("\"")[0];
        String text = json.split("\"text\":\"")[1].split("\"")[0];
        String timestampStr = json.split("\"timestamp\":\"")[1].split("\"")[0];
        ZonedDateTime timestamp = ZonedDateTime.parse(timestampStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return new Message(username, text, timestamp);
    }
}
