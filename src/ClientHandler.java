import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.List;

//Наследуемся от runnable, чтобы запускаться в Threads

/**
 * Класс-обработчик входящих сообщений.
 * Рассылает новые сообщения остальным клиентам и сохраняет в БД
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    //Список всех клиентов нужен чтобы разослать всем новое сообщение
    private List<ClientHandler> clients;
    private PrintWriter out;
    private BufferedReader in;


    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    /**
     * Чтение сообщения, рассылка и сохранения в БД
     */
    @Override
    public void run() {
        try {
            //установка сокета как потоков ввода и вывода
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            //чтение сообщения (из сокета) и обработка
            while ((message = in.readLine()) != null) {
                broadcast(message);
                saveMessageToDB(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
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
    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                //пишем в сокет, который слушают клиенты
                client.out.println(message);
            }
        }
    }

    /**
     * Логирование сообщения в БД
     * @param message - полученное сообщение
     */
    private void saveMessageToDB(String message) {
        String url = "jdbc:mariadb://100.110.2.118:3306/chat";
        String user = "javauser";
        String password = "javapassword";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO messages (username, message, timestamp) VALUES (?, ?, ?)")) {
            //подстановка параметров в запрос
            statement.setString(1,  message.split(":")[0]);
            statement.setString(2, message.split(":")[1]);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            //выполнение запроса
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
