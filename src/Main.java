import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    static ConfigLoader configLoader = new ConfigLoader("application.properties");
    //порт сервера
    private static final int PORT = configLoader.getIntProperty("server.port");
    ;
    //список обработчиков событий
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static Set<String> usernames = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
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