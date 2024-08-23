package main;

import model.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import repository.DatabaseUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//Наследуемся от runnable, чтобы запускаться в Threads

/**
 * Класс-обработчик входящих сообщений.
 * Рассылает сообщения клиентам и сохраняет в БД полученные сообщения
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LogManager.getLogger(Main.class);
    DatabaseUtils databaseUtils;
    private final Socket socket;
    //Список всех клиентов нужен, чтобы разослать всем новое сообщение
    private List<ClientHandler> clients;
    //имя пользователя за которым закрепляется обработчик
    String Username;
    //остальные имена пользователей (для проверки уникальности)
    private Set<String> usernames;
    PrintWriter out;
    BufferedReader in;

    /**
     * Конструктор
     * @param socket - сокет для работы с клиентским приложением
     * @param clients - список других обработчиков для клиентов
     * @param usernames - список имен активных пользователей
     * @param databaseUtils - инструмент работы с бд
     */
    public ClientHandler(Socket socket, List<ClientHandler> clients, Set<String> usernames, DatabaseUtils databaseUtils) {
        logger.info("Initializing Client Handler");
        this.socket = socket;
        this.clients = clients;
        this.usernames = usernames;
        this.databaseUtils = databaseUtils;
    }

    /**
     * Отправка пользователю истории сообщений (при его подключении)
     * если параметр класса historySize >= 0 - то это значение используется. Если оно < 0, то будут получены все сообщения.
     */
    public List<String> getHistory() throws SQLException {
        List<Message> history = new ArrayList<>();
        logger.debug("Getting history");
        try {
            history = databaseUtils.getHistory();
            return history.stream().map(Message::toJson).toList();

        } catch (SQLException e) {
            logger.error(e);
        }
        return List.of();
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
            logger.info("Starting Client Handler");
            //установка сокета как потоков ввода и вывода
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Username = in.readLine();
            logger.debug("Username: " + Username + " want to connect");
            //Регистрация подключения пользователя (проверка, что имя не занято)
            if(registerUser(Username)) {
                //Получение данных из БД и отправка пользователю после регистрации
                for (String message : getHistory()) {
                    out.println(message);
                }
                //получение нового сообщения и обработка
                String jsonMessage;
                while ((jsonMessage = in.readLine()) != null) {
                    handleMessage(Message.fromJson(jsonMessage));
                }
            }
        } catch (IOException | SQLException e) {
            if (e.getMessage().equals("Connection reset")) {
                logger.debug("Client connection closed");
            }
            else {
                logger.error(e);
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Метод "регистрации" пользователя в системе. Имя проверяется на уникальность и если оно уникально - то добавляется в список пользователей
     * @param username - login нового пользователя
     * @return - true при успешной регистрации пользователя, false, если имя занято
     */
    Boolean registerUser(String username){
        // Проверка уникальности имени пользователя
        synchronized (usernames) {
            if (usernames.contains(username)) {
                out.println("Username already taken");
                logger.debug("Username: " + username + " already taken, reject sent");
                return false;
            }
            usernames.add(username);
            logger.debug("User \"{}\" added to chat", username);
            return true;
        }
    }

    /**
     * Метод предназначен для отключения пользователя и освобождения ресурсов. Поток закрывается и удаляется из списка потоков, имя пользователя удаляется из списка, сокет закрывается
     */
    void disconnect(){
        try {
            logger.info("Closing client handler due to client disconnect");
            usernames.remove(Username);
            clients.remove(this);
            socket.close();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * Обработка входящего сообщения.
     * Сохранение в БД, рассылка всем пользователям после сохранения
     * @param message - сообщение которое надо обработать.
     */
    void handleMessage(Message message){
        logger.debug("New incoming message. Message: {}", message.toJson());
        saveMessageToDB(message);
        broadcast(message);
    }

    /**
     * Посылает сообщение всем подключенным клиентам
     * @param message - сообщение для рассылки
     */
    void broadcast(Message message) {
        logger.debug("Start broadcast send new message: {}", message.toJson());
        try {
            Message messageFromDB = databaseUtils.getLastMessage(message.username(), message.text());
            logger.debug("Message from DB: {}", messageFromDB.toJson());
            logger.debug("Sending to clients");
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println(messageFromDB.toJson());
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
        }
    }

    /**
     * Сохранение сообщения в БД
     * @param message - полученное сообщение
     */
    public void saveMessageToDB(Message message) {
        logger.debug("Saving new message to DB. Message: {}", message.toJson());
        try{
            if (databaseUtils.saveMessage(message)) {
                logger.debug("Message saved to database");
            }
        } catch (SQLException e) {
            logger.error(e);
        }
    }
}