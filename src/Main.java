import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class Main {
    static ConfigLoader configLoader = new ConfigLoader("application.properties");
    //порт сервера
    private static final int PORT = configLoader.getIntProperty("server.port");

    //список обработчиков событий
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Set<String> usernames = Collections.synchronizedSet(new HashSet<>());

    public static void initializeDB() {
        String url = configLoader.getProperty("database.url");
        String user = configLoader.getProperty("database.user");
        String password = configLoader.getProperty("database.password");

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            String createTableSQL = "CREATE TABLE IF NOT EXISTS messages ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(255) NOT NULL, "
                    + "message TEXT NOT NULL, "
                    + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")";
            statement.execute(createTableSQL);
            System.out.println("Table 'messages' created or already exists.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        initializeDB();
        //подключаем сервер к сокету
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            //слушаем сокет
            while (true) {
                Socket clientSocket = serverSocket.accept();
                //создаем обработчик на новый запрос
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients, usernames, configLoader);

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