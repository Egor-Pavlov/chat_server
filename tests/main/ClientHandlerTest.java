package main;

import configLoader.ConfigLoader;
import model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import repository.DatabaseUtils;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    private Socket socket;
    private List<ClientHandler> clients;
    private Set<String> usernames;
    private ConfigLoader configLoader;
    private DatabaseUtils databaseUtils;
    private ClientHandler clientHandler;
    private BufferedReader in;
    private PrintWriter out;

    @BeforeEach
    public void setUp() throws Exception {
        socket = mock(Socket.class);
        clients = mock(List.class);
        usernames = mock(Set.class);
        configLoader = mock(ConfigLoader.class);
        databaseUtils = mock(DatabaseUtils.class);

        in = mock(BufferedReader.class);
        out = mock(PrintWriter.class);

        when(socket.getInputStream()).thenReturn(mock(java.io.InputStream.class));
        when(socket.getOutputStream()).thenReturn(mock(java.io.OutputStream.class));

        clientHandler = new ClientHandler(socket, clients, usernames, configLoader, databaseUtils);
        clientHandler.in = in;
        clientHandler.out = out;
    }

    //Источник данных для теста
    private static Stream<Arguments> provideHistoryData() {
        return Stream.of(
            Arguments.of(
                List.of(
                    new Message("tester1", "message1", ZonedDateTime.parse("2024-08-05T12:34:56Z"))
                )
            ),
            Arguments.of(
                List.of(
                    new Message("tester2", "message2", ZonedDateTime.parse("2024-08-06T14:30:00Z")),
                    new Message("tester3", "message3", ZonedDateTime.parse("2024-08-07T16:00:00Z"))
                )
            ),
            Arguments.of(
                List.of(
                    new Message("tester4", "message4", ZonedDateTime.parse("2024-08-08T17:00:00Z")),
                    new Message("tester5", "message5", ZonedDateTime.parse("2024-08-09T18:00:00Z")),
                    new Message("tester6", "message6", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester7", "message7", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester8", "message8", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester9", "message9", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester10", "message10", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester11", "message11", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester12", "message12", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester13", "message13", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester14", "message14", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester15", "message15", ZonedDateTime.parse("2024-08-10T19:00:00Z")),
                    new Message("tester16", "message16", ZonedDateTime.parse("2024-08-10T19:00:00Z"))
                )
            )
        );
    }
    @ParameterizedTest
    @MethodSource("provideHistoryData")
    public void testGetHistory(List<Message> history) throws SQLException {
        //Преобразование массива сообщений в json (метод хэндлера делает это)
        List<String> expectedHistory = history.stream()
                .map(Message::toJson)
                .toList();

        //Учим метод получения из БД отдавать желаемый результат
        when(databaseUtils.getHistory()).thenReturn(history);

        // Вызываем метод
        List<String> actualHistory = clientHandler.getHistory();

        //Проверяем, что он был вызван
        Mockito.verify(databaseUtils, times(1)).getHistory();

        assert(actualHistory.size() == expectedHistory.size());
        //Сравниваем результат
        assertEquals(expectedHistory, actualHistory);
    }
}
