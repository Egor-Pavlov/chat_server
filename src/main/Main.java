package main;

import configLoader.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    static ConfigLoader configLoader = new ConfigLoader("application.properties");
    //порт сервера
    private static final int PORT = configLoader.getIntProperty("server.port");

    //список обработчиков событий
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Set<String> usernames = Collections.synchronizedSet(new HashSet<>());

    // Метод для чтения содержимого файла
    public static String readSQLFile(String filePath) {
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

    public static void initializeDB() {
        logger.info("Initializing database...");
        String url = configLoader.getProperty("database.url");
        String user = configLoader.getProperty("database.user");
        String password = configLoader.getProperty("database.password");

        logger.debug("database url: " + url);
        logger.debug("database user: " + user);
        logger.debug("database password: " + password);

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

    public static void main(String[] args) {
        initializeDB();
        //подключаем сервер к сокету
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
//            System.out.println("Server started on port " + PORT);
            logger.info("Server started on port " + PORT);
            //слушаем сокет
            logger.info("Waiting for connection...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Start new client connection...");
                //создаем обработчик на новый запрос
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients, usernames, configLoader);
                logger.info("New client connected.");
                //добавляем в список
                clients.add(clientHandler);
                //запускаем обработку в отдельном потоке
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}