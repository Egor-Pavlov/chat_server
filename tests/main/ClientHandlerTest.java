package main;

import model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import repository.DatabaseUtils;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    private Socket socket;
    private List<ClientHandler> clients;
    private Set<String> usernames;
    private DatabaseUtils databaseUtils;
    private ClientHandler clientHandler;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Подготовка к тесту. Создание заглушек сокета, списков клиентов и имен активных пользователей
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        socket = mock(Socket.class);
        clients = mock(List.class);
        usernames = mock(Set.class);
        databaseUtils = mock(DatabaseUtils.class);

        in = mock(BufferedReader.class);
        out = mock(PrintWriter.class);

        when(socket.getInputStream()).thenReturn(mock(java.io.InputStream.class));
        when(socket.getOutputStream()).thenReturn(mock(java.io.OutputStream.class));

        clientHandler = new ClientHandler(socket, clients, usernames, databaseUtils);
        clientHandler.in = in;
        clientHandler.out = out;
    }

    /**
     * Источник данных для теста получения истории сообщений. Наборы на 1 и несколько сообщений
     *
     */
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

    /**
     * Тест метода getHistory. Сообщения из метода-источника данных сравниваются с результатом работы метода
     * @param history - сообщения "из БД"
     * @throws SQLException
     */
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

        assert (actualHistory.size() == expectedHistory.size());
        //Сравниваем результат
        assertEquals(expectedHistory, actualHistory);
    }

    /**
     * Тест метода SaveMessageToDB
     * @param testUsername - имя отправителя сообщения
     * @param testText - текст сообщения
     * @param testDate - дата и время отправки
     * @throws SQLException
     */
    @ParameterizedTest
    @CsvSource("tester123, message 123, 2024-08-08T17:00:00Z")
    public void testSaveToDB(String testUsername, String testText, ZonedDateTime testDate) throws SQLException {
        Message message = new Message(testUsername, testText, testDate);
        //заглушка сохранения
        when(databaseUtils.saveMessage(message)).thenReturn(true);
        //вызов
        clientHandler.saveMessageToDB(message);
        //проверка
        verify(databaseUtils, times(1)).saveMessage(message);
    }

    /**
     * Тест метода Broadcsast. Тесовое сообщение рассылается заглушкам клиентов.
     * @param clientsCount - количество клиентов для рассылки
     * @param testUsername - имя отправителя
     * @param testText - текст сообщения
     * @param testDate - дата отправки
     * @throws Exception
     */
    @ParameterizedTest
    @CsvSource("10, tester123, message 123, 2024-08-08T17:00:00Z")
    public void testBroadcast(int clientsCount, String testUsername, String testText, ZonedDateTime testDate) throws Exception {
        Message message = new Message(testUsername, testText, testDate);
        //должна быть не заглушка, а реальный список
        clients = new ArrayList<>();
        // Создаем и настраиваем клиентов
        for (int i = 0; i < clientsCount; i++) {
            Socket socket = mock(Socket.class);
            BufferedReader in = mock(BufferedReader.class);
            PrintWriter out = mock(PrintWriter.class);
            //установка поведения заглушек
            when(socket.getInputStream()).thenReturn(mock(java.io.InputStream.class));
            when(socket.getOutputStream()).thenReturn(mock(java.io.OutputStream.class));
            when(databaseUtils.getLastMessage(anyString(), anyString())).thenReturn(message);

            ClientHandler clientHandler = new ClientHandler(socket, clients, usernames, databaseUtils);
            clientHandler.in = in;
            clientHandler.out = out;

            clients.add(clientHandler);
        }

        // Вызываем метод broadcast для одного из клиентов
        //имитация того, что сообщение пришло от клиента № 0 и обработчик клиента 0 рассылает всем остальным
        clients.get(0).broadcast(message);

        // Проверка, что сообщение было отправлено всем клиентам
        for (ClientHandler client : clients) {
            verify(client.out, times(1)).println(message.toJson());
        }
    }

    /**
     * Тест метода Disconnect. Пользователь создается и после проверяется, что он удален из списков обработчиков и имен активных пользователей
     * @param username
     */
    @ParameterizedTest
    @CsvSource("Antonio Banderos")
    public void testDisconnect(String username) {
        clients = new ArrayList<>();
        usernames = new HashSet<>();
        //создаем обработчик клиента
        ClientHandler clientHandler = new ClientHandler(socket, clients, usernames, databaseUtils);
        clientHandler.Username = username;
        //добавляем в список обработчиков и имен пользователей
        clients.add(clientHandler);
        usernames.add(username);
        //вызываем метод отключения
        clientHandler.disconnect();
        //Проверяем
        assertTrue(clients.isEmpty());
        assert (!usernames.contains(username));
    }

    /**
     * Тест регистрации пользователя при подключении. Создается имя пользователя в массиве активных и имитируется подключение нового.
     * Есть сценарий, когда имя свободно и занято.
     * @param existedUsername
     * @param newUsername
     * @param expectedResult
     * @throws IOException
     */
    @ParameterizedTest
    @CsvSource({"Tester, Tester, false",
                "Tester, Tester123, true"})
    public void testRegisterUser(String existedUsername, String newUsername, boolean expectedResult) throws IOException {
        //Заглушка потока вывода
        PrintWriter out = mock(PrintWriter.class);
        when(socket.getOutputStream()).thenReturn(mock(java.io.OutputStream.class));
        //добавление существующего имени пользователя
        usernames = new HashSet<>();
        usernames.add(existedUsername);
        //создание нового обработчика
        ClientHandler clientHandler = new ClientHandler(socket, clients, usernames, databaseUtils);
        clientHandler.out = out;
        //Проверка регистрации с указанным именем пользователя
        assertEquals(expectedResult, clientHandler.registerUser(newUsername));
    }
}