package com.storeya.shop.utils;

import java.security.SecureRandom;
import java.util.Random;

public class StringRandom {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random random = new SecureRandom();

    public static String generateStringRandom(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Độ dài phải lớn hơn 0");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

}
