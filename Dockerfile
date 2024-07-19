# back
# устанавливаем самую лёгкую версию JVM
FROM openjdk:17-jdk-alpine

# указываем ярлык. Например, разработчика образа и проч. Необязательный пункт.
LABEL maintainer="egor-pavlov"

# указываем точку монтирования для внешних данных внутри контейнера (как мы помним, это Линукс)
VOLUME /etc/chat-server

# внешний порт, по которому наше приложение будет доступно извне
EXPOSE 1337

# указываем, где в нашем приложении лежит джарник
ARG JAR_FILE=out/artifacts/chat_SE_jar/chat_SE.jar

# добавляем джарник в образ
ADD ${JAR_FILE} out/artifacts/chat_SE_jar/chat_SE.jar

# команда запуска джарника
ENTRYPOINT ["java","-jar","out/artifacts/chat_SE_jar/chat_SE.jar"]