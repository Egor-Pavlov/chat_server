package main;

import configLoader.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import repository.DatabaseUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Точка входа в серверную часть
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    static ConfigLoader configLoader = new ConfigLoader("application.properties");
    static DatabaseUtils databaseUtils = new DatabaseUtils(configLoader);
    //порт сервера
    private static final int PORT = configLoader.getIntProperty("server.port");

    //список обработчиков событий
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Set<String> usernames = Collections.synchronizedSet(new HashSet<>());

    /**
     * Чтение конфигурации и запуск сервера, создание обработчиков при подключении клиентов
     * @param args
     */
    public static void main(String[] args) {
        databaseUtils.initializeDB();
        //подключаем сервер к сокету
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port " + PORT);
            //слушаем сокет
            logger.info("Waiting for connection...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Start new client connection...");
                //создаем обработчик на новый запрос
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients, usernames, databaseUtils);
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