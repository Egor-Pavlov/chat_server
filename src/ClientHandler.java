import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.List;
import java.util.Set;

//Наследуемся от runnable, чтобы запускаться в Threads

/**
 * Класс-обработчик входящих сообщений.
 * Рассылает новые сообщения остальным клиентам и сохраняет в БД
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    //Список всех клиентов нужен, чтобы разослать всем новое сообщение
    private List<ClientHandler> clients;
    private String Username;
    private Set<String> usernames;
    private PrintWriter out;
    private BufferedReader in;

    private String url = "jdbc:mariadb://100.110.2.118:3306/chat";
    private String user = "javauser";
    private String password = "javapassword";
    private int historySize = 10;

    public ClientHandler(Socket socket, List<ClientHandler> clients, Set<String> usernames) {
        this.socket = socket;
        this.clients = clients;
        this.usernames = usernames;
    }

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
                String time = resultSet.getString("timestamp");
                String username = resultSet.getString("username");
                String message = resultSet.getString("message");
                out.println(time + "|" + username + "|" + message);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
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

            String message;
            //чтение сообщения (из сокета) и обработка
            while ((message = in.readLine()) != null) {
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
    private void broadcast(String message) {
        String query = "SELECT * FROM messages where username='" + message.split("\\|")[0] + "' and message='" + message.split("\\|")[1] + "' ORDER BY id DESC LIMIT 1;";
        String messagewithTime = "";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String time = resultSet.getString("timestamp");
                String username = resultSet.getString("username");
                String text = resultSet.getString("message");
                messagewithTime = time + "|" + username + "|" + text;
            }
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    //пишем в сокет, который слушают клиенты
                    client.out.println(messagewithTime);
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
    private void saveMessageToDB(String message) {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO messages (username, message, timestamp) VALUES (?, ?, ?)")) {
            //подстановка параметров в запрос
            String username = "anonymous";
            String text = "";
            if (message.contains("|")) {
                username = message.split("\\|")[0];
                text = message.split("\\|")[1];
            }
            statement.setString(1,  username);
            statement.setString(2, text);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            //выполнение запроса
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
