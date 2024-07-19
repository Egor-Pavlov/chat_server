## Содержание
* [Описание](#description)
* [Сборка](#build)
* [Тесты](#tests)
* [Описание классов](#classes)
* [Журналирование](#logs)
* [Клиентская часть](#client)

<a name="description"><h1>Описание</h1></a>
Проект является мини лабораторной работой по изучению многопоточности и взаимодействия с сетью на примере многопользовательского оналйн чата. В данном проекте реализована серверная часть приложения.  
* Используется язык JAVA, без фреймворков. 
* Многопоточность реализована с помощью Threads. 
* В качестве СУБД используется MariaDB.
* Предусмотрен запуск в докер-контейнере.

<a name="build"><h2>Сборка и запуск</h2></a>
1. Собрать проект в jar средствами ide (_build_ -> _build artifacts_) 
2. Выполнить сборку образа docker
   ```bash
    docker build -t chat-server .
   ```
3. Создать файл `docker-compose.yml`. Пример содержимого:
  ```yml
    version: "3.8"
    services:
        chat-server:
          image: chat-server
          container_name: chat-server
          ports:
            - "1337:1337"
          environment:
            - server.port=1337
            - database.url=jdbc:mariadb://100.110.2.118:3306/chat
            - database.user=javauser
            - database.password=javapassword
            - history.size=10
          restart: always
  ```
4. Запустить контейнер  
   ```bash
   docker sudo docker compose up -d
   ```

Для запуска приложения через IDE нужно отредактировать конфигурационный файл application.properties в /resources и запустить файл `configLoader.java`

Пример конфигурации:
```properties
server.port=12345
database.url=jdbc:mariadb://100.110.2.118:3306/chat
database.user=javauser
database.password=javapassword
history.size=10
```
Нужно указать путь к БД, логин и пароль пользователя БД с правами доступа, и количество старых сообщений, которые отправятся клиенту при подключении к серверу. 

<a name="tests"><h2>Тесты</h2></a>

<a name="classes"><h1>Описание классов</h1></a>
## main.Main.java
Класс содержит подключение к порту, прослушивание и обработку входящих сообщений в отдельных потоках с помощью Threads
## main.ClientHandler.java
Наследуется от runnable для запуска в отдельном потоке, не возвращает результат работы. Обрабатывает полученные сообщения, записывает сообщения в БД в формате
```sql
MariaDB [chat]> select * from messages;
+----+-----------+------------------+---------------------+------------------+
| id | username  | message          | timestamp           | timezone         |
+----+-----------+------------------+---------------------+------------------+
|  1 | Admin     | привет           | 2021-06-23 01:45:29 | Europe/London    |
|  2 | anonymous | Проверка         | 2024-07-16 10:27:11 | Asia/Novosibirsk |
+----+-----------+------------------+---------------------+------------------+

```
При подключении нового клиента проверяется уникальность его имени пользователя, и если оно уникально - ему присылается история сообщений
Сообщения между клиентом и сервером передаются в формате json
Пример получаемого сообщения: 
```json
{
  "username":"anonymous",
  "text":"Проверка",
  "timestamp":"2024-07-16T10:27:11+07:00[Asia/Novosibirsk]"
}
```
Полученное сообщение рассылается всем подключенным клиентам. В бд запишется имя пользователя и текст
После записи в БД выполняется рассылка полученного сообщения клиентам. Клиентские приложения отображают сообщения на экране.

## Схема БД
При старте сервис создает таблицу messages. Для успешной инициализации таблицы БД и пользователь должны существовать и обладать корректными правами
Схема приложена в файле /src/schema.sql
```sql
CREATE TABLE IF NOT EXISTS messages (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          username VARCHAR(255) NOT NULL,
                          message TEXT NOT NULL,
                          timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          timezone VARCHAR(50) DEFAULT NULL
);
```

<a name="clients"><h1>Клиентская часть описана в проекте chat_client</h1></a>

https://github.com/Egor-Pavlov/chat_client
