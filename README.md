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

<details>
  <summary>Структура проекта</summary>

```bash
├.
├── lib
│   ├── apache-log4j-2.23.1-bin.zip
│   ├── byte-buddy-1.12.22.jar
│   ├── byte-buddy-agent-1.12.22.jar
│   ├── log4j-api-2.23.1.jar
│   ├── log4j-core-2.23.1.jar
│   ├── log4j-docker-2.23.1.jar
│   ├── mariadb-java-client-3.4.0.jar
│   ├── mockito-core-5.0.0.jar
│   ├── mockito-junit-jupiter-5.0.0.jar
│   └── mysql-connector-j-9.0.0.jar
├── out
│   ├── artifacts
│   │   └── chat_SE_jar
│   │       └── chat_SE.jar
│   └── production
│       └── chat_SE
│           ├── configLoader
│           │   └── ConfigLoader.class
│           ├── main
│           │   ├── ClientHandler.class
│           │   └── Main.class
│           ├── META-INF
│           │   └── MANIFEST.MF
│           ├── model
│           │   └── Message.class
│           ├── application.properties
│           ├── log4j2.xml
│           └── schema.sql
├── resources
│   ├── META-INF
│   │   └── MANIFEST.MF
│   ├── application.properties
│   ├── log4j2.xml
│   └── schema.sql
├── src
│   ├── configLoader
│   │   └── ConfigLoader.java
│   ├── main
│   │   ├── ClientHandler.java
│   │   └── Main.java
│   ├── model
│   │   └── Message.java
│   └── repository
│       └── DatabaseUtils.java
├── tests
│   ├── main
│   │   └── ClientHandlerTest.java
│   └── model
│       └── MessageTest.java
├── chat_SE.iml
├── Dockerfile
└── README.md

```
</details>


<a name="build"><h2>Сборка и запуск</h2></a>
1. Собрать проект в jar средствами ide (_build_ -> _build artifacts_) 
2. Выполнить сборку образа docker (команду выполнять в корне проекта)
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
   sudo docker compose up -d
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
## Сценарии тестов

### Message Class

#### Сценарий теста для Message.fromJson(String json)

* Описание: Убедитесь, что метод правильно парсит JSON-строку и создает объект Message.
* Входные данные: Валидная JSON-строка, представляющая объект Message.
* Ожидаемый результат: Объект Message с правильными значениями полей.

#### Сценарий теста для Message.toJson()

* Описание: Убедитесь, что метод правильно преобразует объект Message в JSON-строку.
* Входные данные: Объект Message с установленными значениями полей.
* Ожидаемый результат: Корректная JSON-строка, представляющая объект Message.

#### Сценарий теста для Message.get...()
Сравнение значения в тестовом объекте со значением в переменной, которая использовалась как источник данных для поля при создании объекта

### ClientHandler Class

#### Сценарий теста для ClientHandler.run()

* Описание: Убедитесь, что метод правильно обрабатывает входящие сообщения.
* Входные данные: Мок-объект BufferedReader, возвращающий последовательность строк (включая сообщение о занятом имени пользователя и обычные сообщения).
* Ожидаемый результат: Соответствующая обработка сообщений и вызовы методов GUI (например, отображение сообщения об ошибке или обновление текстового поля).

#### Обработка занятого имени пользователя

* Описание: Убедитесь, что сообщение "Username already taken" отправляется правильно.
* Входные данные: Массив с подключенным пользователем, запрос на подключение пользователя с именем которое занято.
* Ожидаемый результат: отправка сообщения "Username already taken".

#### Обработка обычных сообщений пользователей

* Описание: Убедитесь, что обычные сообщения пользователей обрабатываются и сохраняются в БД правильно.
* Входные данные: JSON-строка, представляющая объект Message.
* Ожидаемый результат: Правильное отображение сообщения в БД.

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

<a name="client"><h1>Клиентская часть описана в проекте chat_client</h1></a>

https://github.com/Egor-Pavlov/chat_client
