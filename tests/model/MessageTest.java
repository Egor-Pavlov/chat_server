package model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    private static final Logger log = LogManager.getLogger(MessageTest.class);
    static ZonedDateTime timestamp;
    static Message testMessage;
    static String testUsername = "Antonio Banderos";
    static String testMessageText = "This is a test message";

    @BeforeAll
    public static void createTestMessage() {
        timestamp = ZonedDateTime.now();
        testMessage = new Message(testUsername, testMessageText, timestamp);
    }

    @Test
    void toJson() {
        assertEquals("{\"username\":\""+ testUsername +
                        "\",\"text\":\""+ testMessageText +
                        "\",\"timestamp\":\"" + timestamp.format(DateTimeFormatter.ISO_ZONED_DATE_TIME) +
                        "\"}",
                testMessage.toJson());
    }

    @ParameterizedTest
    @CsvSource({
            "Antonio Banderos, This is a test message, 2024-08-05T12:34:56Z ,true",
            "Antonio Banderos, This is a test message, incorrect date, false",
            "Antonio Banderos, This is a test message, , false"
    })
    void fromJson(String testUsername, String testMessageText, String testDate, boolean isValid) {
        String json = String.format("{\"username\":\"%s\",\"text\":\"%s\",\"timestamp\":\"%s\"}",
                testUsername, testMessageText, testDate);
        System.out.println(json);
        if (isValid) {
            Message newTestMessage = Message.fromJson(json);
            assertEquals(testUsername, newTestMessage.getUsername());
            assertEquals(testMessageText, newTestMessage.getText());
            assertEquals(ZonedDateTime.parse(testDate), newTestMessage.getTimestamp());
        } else {
            assertThrows(IllegalArgumentException.class, () -> {
                Message.fromJson(json);
            });
        }
    }

    @ParameterizedTest
    @MethodSource("provideInvalidMessageParameters")
    void invalidMessageParameters(String username, String text, ZonedDateTime timestamp) {
        assertThrows(NullPointerException.class, () -> {
            new Message(username, text, timestamp);
        });
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideInvalidMessageParameters() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(null, testMessageText, timestamp),
                org.junit.jupiter.params.provider.Arguments.of(testUsername, null, timestamp),
                org.junit.jupiter.params.provider.Arguments.of(testUsername, testMessageText, null)
        );
    }

    @Test
    void getUsername() {
        assertEquals(testUsername, testMessage.getUsername());
    }

    @Test
    void getText() {
        assertEquals(testMessageText, testMessage.getText());
    }

    @Test
    void getTimestamp() {
        assertEquals(timestamp, testMessage.getTimestamp());
    }
}
