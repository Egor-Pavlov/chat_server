package model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    static ZonedDateTime timestamp;
    static Message testMessage;
    static String testUsername = "Antonio Banderos";
    static String testMessageText = "This is a test message";

    /**
     * Создание тестового объекта на основе имени пользователя, сообщения и даты и времени
     */
    @BeforeAll
    public static void createTestMessage() {
        timestamp = ZonedDateTime.now();
        testMessage = new Message(testUsername, testMessageText, timestamp);
    }

    /**
     * Создается строка на основе тетсовых имени пользователя, текста и даты-времени.
     * Вызывается toJson у тестового объекта, строки сравниваются
     */
    @Test
    void toJson() {
        assertEquals("{\"username\":\""+ testUsername +
                        "\",\"text\":\""+ testMessageText +
                        "\",\"timestamp\":\"" + timestamp.toString() +
                        "\"}",
                testMessage.toJson());
    }

    /**
     * Получаем json строку из созданного сообщения
     * делаем на ее основе новый объект методом fromJson
     * Проверяем, что данные в новом объекте соответствуют необходимым
     *
     * Недостаток - все сломается если не работает toJson, но я не хочу задавать строку константой и хз как правильно
     */
    @Test
    void fromJson() {
        String json = testMessage.toJson();
        Message newTestMessage = Message.fromJson(json);
        assertEquals(testMessage.getUsername(), newTestMessage.getUsername());
        assertEquals(testMessage.getText(), newTestMessage.getText());
        assertEquals(timestamp, newTestMessage.getTimestamp());
    }

    /**
     * Проверка геттера для имени пользователя
     */
    @Test
    void getUsername() {
        assertEquals(testUsername, testMessage.getUsername());
    }
    /**
     * Проверка геттера для текста сообщения
     */
    @Test
    void getText() {
        assertEquals(testMessageText, testMessage.getText());
    }
    /**
     * Проверка геттера для даты и времени отправки сообщения
     */
    @Test
    void getTimestamp() {
        assertEquals(timestamp, testMessage.getTimestamp());
    }
}