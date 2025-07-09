package utils;

import java.util.*;

public class Token {
    private static Map<String, User> allTokens = new HashMap<>();
    public static String generateToken(User user) {
        String token = UUID.randomUUID().toString();
        allTokens.put(token, user);
        return token;
    }
    public static User validateToken(String token) {
        return allTokens.get(token);
    }

    public static void removeToken(String token) {
        allTokens.remove(token);
    }
}
