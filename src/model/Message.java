package model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Представление сущности "сообщение".
 * Reсord используется, так как объект не изменяемый.
 * @param username
 * @param text
 * @param timestamp
 */
public record Message(String username, String text, ZonedDateTime timestamp) {
    /**
     * Конструктор со всеми параметрами объекта.
     * @param username - имя отправителя
     * @param text - текст сообщения
     * @param timestamp - дата и время отправки
     */
    public Message(String username, String text, ZonedDateTime timestamp) {
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.text = Objects.requireNonNull(text, "Text cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    /**
     * Преобразования в json-строку
     * @return - строка json, содержащая данные объекта
     */
    public String toJson() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        return String.format("{\"username\":\"%s\",\"text\":\"%s\",\"timestamp\":\"%s\"}",
                username, text, timestamp.format(formatter));
    }

    /**
     * Получение объекта из json
     * @param json - строка json, описывающая объект
     * @return - новый объект класса Message
     */
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
