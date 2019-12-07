package utils;

import java.util.UUID;

public class Utils {

    public static String getIPLocal() {
        return "192.168.33.5";
    }

    public static String generateString() {
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

}
