# Используем самую лёгкую версию JVM
FROM openjdk:17-jdk-alpine

# Указываем ярлык, например, разработчика образа и прочее
LABEL maintainer="egor-pavlov"

# Указываем точку монтирования для внешних данных внутри контейнера
VOLUME /etc/chat-server

# Внешний порт, по которому наше приложение будет доступно извне
EXPOSE 1337

# Указываем, где в нашем приложении лежит JAR файл
ARG JAR_FILE=out/artifacts/chat_SE_jar/chat_SE.jar

# Копируем JAR файл в контейнер
COPY ${JAR_FILE} /app/chat_SE.jar

# Копируем все зависимости из папки lib в контейнер
COPY lib /app/lib

# Копируем конфигурационные файлы, если они требуются в runtime
COPY resources/application.properties /app/application.properties
COPY resources/log4j2.xml /app/log4j2.xml

# Команда запуска JAR файла с указанием classpath
ENTRYPOINT ["java","-cp","/app/chat_SE.jar:/app/lib/*","main.Main"]
