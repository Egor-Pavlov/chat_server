package repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import configLoader.ConfigLoader;
import main.Main;
import model.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseUtils {

    private static final Logger logger = LogManager.getLogger(Main.class);
    private ConfigLoader configLoader;

    private String url = "jdbc:mariadb://127.0.0.1:3306/chat";
    private String user = "javauser";
    private String password = "javapassword";
    private int historySize = 10;

    public DatabaseUtils(ConfigLoader configLoader) {
        logger.info("Initializing database...");
        if (configLoader != null) {
            this.configLoader = configLoader;
            url = configLoader.getProperty("database.url");
            user = configLoader.getProperty("database.user");
            password = configLoader.getProperty("database.password");
            historySize = Integer.parseInt(configLoader.getProperty("history.size"));
        }
        else {

            logger.error("Start with default configuration");
        }

        logger.debug("database url: " + url);
        logger.debug("database user: " + user);
        logger.debug("database password: " + password);
        logger.debug("database history size: " + historySize);
    }

    public List<Message> getHistory() throws SQLException {
        List<Message> history = new ArrayList<>();
        String query = "SELECT * FROM messages";
        if (historySize >= 0) {
            query = "SELECT * FROM (SELECT * FROM messages ORDER BY id DESC LIMIT " + historySize + ") subquery ORDER BY id ASC;";
        }
        logger.debug("query: " + query);

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp("timestamp");
                String timezone = resultSet.getString("timezone");
                ZoneId zoneId = ZoneId.of(timezone);
                ZonedDateTime dateTime = ZonedDateTime.ofInstant(timestamp.toInstant(), zoneId);

                Message message = new Message(resultSet.getString("username"), resultSet.getString("message"), dateTime);
                history.add(message);
            }
        }
        return history;
    }
    public Message getLastMessage(String username, String messageText) throws SQLException {
        String query = "SELECT * FROM messages WHERE username=? AND message=? ORDER BY id DESC LIMIT 1;";
        Message message = null;

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, username);
            statement.setString(2, messageText);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp("timestamp");
                    String timezone = resultSet.getString("timezone");
                    ZoneId zoneId = ZoneId.of(timezone);
                    ZonedDateTime dateTime = ZonedDateTime.of(timestamp.toLocalDateTime(), zoneId);

                    message = new Message(username, messageText, dateTime);
                }
            }
        }
        return message;
    }

    public boolean saveMessage(Message message) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO messages (username, message, timestamp, timezone) VALUES (?, ?, ?, ?)")) {

            statement.setString(1, message.getUsername());
            statement.setString(2, message.getText());

            // Преобразование ZonedDateTime в Timestamp
            Timestamp timestamp = Timestamp.from(message.getTimestamp().toInstant());
            statement.setTimestamp(3, timestamp);

            // Добавление таймзоны
            statement.setString(4, message.getTimestamp().getZone().getId());
            logger.debug("query:" + statement);
            statement.executeUpdate();
            return true;
        }
    }

    // Метод для чтения содержимого файла
    public String readSQLFile(String filePath) {
        logger.debug("Reading SQL file: " + filePath);
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found! " + filePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void initializeDB() {
        logger.info("Connecting to database...");
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            String createTableSQL = readSQLFile("schema.sql");
            statement.execute(createTableSQL);
            logger.info("Table 'messages' created or already exists.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.info("Database initialized and connected.");
    }
}