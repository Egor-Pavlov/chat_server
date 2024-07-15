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
    //Список всех клиентов нужен, чтобы разослать всем новое сообщение
    private List<ClientHandler> clients;
    private PrintWriter out;
    private BufferedReader in;

    private String url = "jdbc:mariadb://100.110.2.118:3306/chat";
    private String user = "javauser";
    private String password = "javapassword";

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    public String getHistory(){
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM messages");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String message = resultSet.getString("message");
                out.println(username + ":"+ message);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
    /**
     * Чтение сообщения, рассылка и сохранения в БД
     * Создание и запуск потока: Каждый раз при подключении нового клиента создается новый поток, который выполняет код из метода run() в ClientHandler.
     * Работа потока: Поток читает сообщения от клиента, рассылает их всем другим клиентам через метод broadcast() и сохраняет в базу данных через saveMessageToDB().
     * Завершение потока: Поток завершается, когда закрывается соединение с клиентом (socket.close()), что вызывает выход из цикла while в run() методе ClientHandler.
     */
    @Override
    public void run() {
        try {
            //установка сокета как потоков ввода и вывода
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            //Получение данных из БД
            getHistory();
            //Преобразование



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
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO messages (username, message, timestamp) VALUES (?, ?, ?)")) {
            //подстановка параметров в запрос
            String username = "anonymous";
            if (message.contains(":")) {
                username = message.split(":")[0];
                message = message.split(":")[1];
            }
            statement.setString(1,  username);
            statement.setString(2, message);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            //выполнение запроса
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
