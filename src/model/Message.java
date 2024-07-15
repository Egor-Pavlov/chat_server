package model;

import java.sql.Timestamp;

public class Message {
    private String username;
    private String text;
    private Timestamp timestamp;

    public Message(String username, String text, long timestamp) {
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters and setters
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

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    // сериализация в json
    public String toJson() {
        return String.format("{\"username\":\"%s\",\"text\":\"%s\",\"timestamp\":%d}", username, text, timestamp);
    }

    // де сериализация из json
    public static Message fromJson(String json) {
        String username = json.split("\"username\":\"")[1].split("\"")[0];
        String text = json.split("\"text\":\"")[1].split("\"")[0];
        long timestamp = Long.parseLong(json.split("\"timestamp\":")[1].split("}")[0]);
        return new Message(username, text, timestamp);
    }
}
