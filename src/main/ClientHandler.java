package main;

import configLoader.ConfigLoader;
import model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

//Наследуемся от runnable, чтобы запускаться в Threads

/**
 * Класс-обработчик входящих сообщений.
 * Рассылает сообщения клиентам и сохраняет в БД полученные сообщения
 */
public class ClientHandler implements Runnable {
    private ConfigLoader configLoader;
    private Socket socket;
    //Список всех клиентов нужен, чтобы разослать всем новое сообщение
    private List<ClientHandler> clients;
    private String Username;
    private Set<String> usernames;
    private PrintWriter out;
    private BufferedReader in;

    private String url = "jdbc:mariadb://127.0.0.1:3306/chat";
    private String user = "javauser";
    private String password = "javapassword";
    private int historySize = 10;

    public ClientHandler(Socket socket, List<ClientHandler> clients, Set<String> usernames, ConfigLoader configLoader) {
        this.socket = socket;
        this.clients = clients;
        this.usernames = usernames;
        this.configLoader = configLoader;

        url = configLoader.getProperty("database.url");
        user = configLoader.getProperty("database.user");
        password = configLoader.getProperty("database.password");
        historySize = Integer.parseInt(configLoader.getProperty("history.size"));
    }

    /**
     * Отправка пользователю истории сообщений (при его подключении)
     * если параметр класса historySize >= 0 - то это значение используется. Если оно < 0, то будут получены все сообщения.
     */
    public void getHistory(){

        String query = "SELECT * FROM messages";
        if(historySize >= 0){
            query = "SELECT * FROM (SELECT * FROM messages ORDER BY id DESC LIMIT "+
                    historySize + ") subquery ORDER BY id ASC;";
        }

        try (Connection connection = DriverManager.getConnection(url, user, password);
            PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp("timestamp");

                //в столбце timestamp хранится время по серверу, в таймзоне - часовой пояс в который надо преобразовать время
                String timezone = resultSet.getString("timezone");

                ZoneId zoneId = ZoneId.of(timezone);
                //Время преобразуется в указанный пояс и отправляется. Получатель преобразует время в свой часовой пояс и отображает
                ZonedDateTime dateTime = ZonedDateTime.ofInstant(timestamp.toInstant(), zoneId);


                Message messageFromDB = new Message(resultSet.getString("username"),
                        resultSet.getString("message"), dateTime);

                out.println(messageFromDB.toJson());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Чтение сообщения, рассылка и сохранения в БД
     * Создание и запуск потока: Каждый раз при подключении нового клиента создается новый поток, который выполняет код из метода run() в main.ClientHandler.
     * Работа потока: Поток читает сообщения от клиента, рассылает их всем другим клиентам через метод broadcast() и сохраняет в базу данных через saveMessageToDB().
     * Завершение потока: Поток завершается, когда закрывается соединение с клиентом (socket.close()), что вызывает выход из цикла while в run() методе main.ClientHandler.
     */
    @Override
    public void run() {
        try {
            //установка сокета как потоков ввода и вывода
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Username = in.readLine();
            // Проверка уникальности имени пользователя
            synchronized (usernames) {
                if (usernames.contains(Username)) {
                    out.println("Username already taken");
                    return;
                }
                usernames.add(Username);
            }

            //Получение данных из БД и отправка пользователям
            getHistory();

            String jsonMessage;
            while ((jsonMessage = in.readLine()) != null) {
                Message message = Message.fromJson(jsonMessage);
                saveMessageToDB(message);
                broadcast(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                usernames.remove(Username);
                clients.remove(this);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Посылает сообщение всем подключенным клиентам
     * @param message - сообщение для рассылки
     */
    private void broadcast(Message message) {
        String query = "SELECT * FROM messages WHERE username=? AND message=? ORDER BY id DESC LIMIT 1;";
        Message messageFromDB = null;

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, message.getUsername());
            statement.setString(2, message.getText());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String username = resultSet.getString("username");
                String text = resultSet.getString("message");
                Timestamp timestamp = resultSet.getTimestamp("timestamp");
                String timezone = resultSet.getString("timezone");

                ZonedDateTime dateTime = ZonedDateTime.of(timestamp.toLocalDateTime(), ZoneId.of(timezone));
                messageFromDB = new Message(username, text, dateTime);
            }

            if (messageFromDB != null) {
                synchronized (clients) {
                    for (ClientHandler client : clients) {
                        client.out.println(messageFromDB.toJson());
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Логирование сообщения в БД
     * @param message - полученное сообщение
     */
    private void saveMessageToDB(Message message) {
        String url = "jdbc:mariadb://100.110.2.118:3306/chat";
        String user = "javauser";
        String password = "javapassword";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO messages (username, message, timestamp, timezone) VALUES (?, ?, ?, ?)")) {

            statement.setString(1, message.getUsername());
            statement.setString(2, message.getText());

            // Преобразование ZonedDateTime в Timestamp
            Timestamp timestamp = Timestamp.from(message.getTimestamp().toInstant());
            statement.setTimestamp(3, timestamp);

            // Добавление таймзоны
            statement.setString(4, message.getTimestamp().getZone().getId());

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//    public List<Message> getMessagesFromDB() {
//        List<Message> messages = new ArrayList<>();
//        String url = "jdbc:mariadb://100.110.2.118:3306/chat";
//        String user = "javauser";
//        String password = "javapassword";
//
//        try (Connection connection = DriverManager.getConnection(url, user, password);
//             PreparedStatement statement = connection.prepareStatement("SELECT username, message, timestamp, timezone FROM messages");
//             ResultSet resultSet = statement.executeQuery()) {
//
//            while (resultSet.next()) {
//                String username = resultSet.getString("username");
//                String text = resultSet.getString("message");
//                Timestamp timestamp = resultSet.getTimestamp("timestamp");
//                String timezone = resultSet.getString("timezone");
//
//                // Преобразование Timestamp и таймзоны в ZonedDateTime
//                ZoneId zoneId = ZoneId.of(timezone);
//                ZonedDateTime dateTime = ZonedDateTime.ofInstant(timestamp.toInstant(), zoneId);
//
//                messages.add(new Message(username, text, dateTime));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return messages;
//    }

}