package util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public class JDBCUtil {
    private static Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("jdbc.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getValue(String key) {
        return properties.getProperty(key);
    }
}
