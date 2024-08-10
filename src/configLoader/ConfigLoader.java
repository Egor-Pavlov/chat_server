package configLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Класс для работы с конфигурационным файлом
 */
public class ConfigLoader {
    private final Properties properties;

    /**
     * Открытие файла
     * @param fileName - имя файла конфигурации
     */
    public ConfigLoader(String fileName) {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + fileName);
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Получение параметра в виде строки
     * @param key - ключ (название параметра)
     * @return - значение параметра
     */
    public String getProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return value;
    }

    /**
     * Получение параметра в виде целого числа
     * @param key - ключ (название параметра)
     * @return - значение параметра
     */
    public int getIntProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return Integer.parseInt(value);
    }
}