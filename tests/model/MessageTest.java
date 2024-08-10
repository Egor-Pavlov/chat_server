package model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты класса Message (работа с сообщениями)
 */
class MessageTest {
    static ZonedDateTime timestamp;
    static Message testMessage;
    static String testUsername = "Antonio Banderos";
    static String testMessageText = "This is a test message";

    /**
     * Подготовка - создание тестового сообщения
     */
    @BeforeAll
    public static void createTestMessage() {
        timestamp = ZonedDateTime.now();
        testMessage = new Message(testUsername, testMessageText, timestamp);
    }

    /**
     * Тест метода преобразования в json.
     * Тестовое сообщение преобразуется в json строку внутренним методом и подстановкой. Значения должны совпадать
     */
    @Test
    void toJson() {
        assertEquals("{\"username\":\""+ testUsername +
                        "\",\"text\":\""+ testMessageText +
                        "\",\"timestamp\":\"" + timestamp.format(DateTimeFormatter.ISO_ZONED_DATE_TIME) +
                        "\"}",
                testMessage.toJson());
    }

    /**
     * Тест метода преобразования из json.
     * В наборах данных есть следующие варианты: данные корректны, дата некорректного формата, дата отсутствует
     * @param testUsername
     * @param testMessageText
     * @param testDate
     * @param isValid
     */
    @ParameterizedTest
    @CsvSource({
            "Antonio Banderos, This is a test message, 2024-08-05T12:34:56Z ,true",
            "Antonio Banderos, This is a test message, incorrect date, false",
            "Antonio Banderos, This is a test message, , false"
    })
    void fromJson(String testUsername, String testMessageText, String testDate, boolean isValid) {
        String json = String.format("{\"username\":\"%s\",\"text\":\"%s\",\"timestamp\":\"%s\"}",
                testUsername, testMessageText, testDate);
        //если исключения быть не должно (данные корректны)
        if (isValid) {
            //создаем объект
            Message newTestMessage = Message.fromJson(json);
            //проверяем что данные заехали правильно
            assertEquals(testUsername, newTestMessage.username());
            assertEquals(testMessageText, newTestMessage.text());
            assertEquals(ZonedDateTime.parse(testDate), newTestMessage.timestamp());
        }
        //ловим исключение, если данные заведомо некорректные
        else {
            assertThrows(IllegalArgumentException.class, () -> Message.fromJson(json));
        }
    }

    /**
     * Источник данных для теста конструктора Message
     * @return - поток данных
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideInvalidMessageParameters() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(null, testMessageText, timestamp),
                org.junit.jupiter.params.provider.Arguments.of(testUsername, null, timestamp),
                org.junit.jupiter.params.provider.Arguments.of(testUsername, testMessageText, null)
        );
    }

    /**
     * Тест создания объекта через конструктор с отсутствием одного из значений
     * @param username - имя пользователя
     * @param text - текст сообщения
     * @param timestamp - дата отправки
     */
    @ParameterizedTest
    @MethodSource("provideInvalidMessageParameters")
    void invalidMessageParameters(String username, String text, ZonedDateTime timestamp) {
        assertThrows(NullPointerException.class, () -> new Message(username, text, timestamp));
    }

    /**
     * Тест получения имени пользователя
     */
    @Test
    void getUsername() {
        assertEquals(testUsername, testMessage.username());
    }

    /**
     * Тест получения текста сообщения
     */
    @Test
    void getText() {
        assertEquals(testMessageText, testMessage.text());
    }

    /**
     * Тест получения даты и времени отправки
     */
    @Test
    void getTimestamp() {
        assertEquals(timestamp, testMessage.timestamp());
    }
}