package configLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private Properties properties;

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

    public String getProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return value;
    }

    public int getIntProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return Integer.parseInt(value);
    }
}
