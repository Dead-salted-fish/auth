package com.lld.auth.utils;

import java.security.SecureRandom;
import java.util.Random;

public class TokenGenerator {

    private static final String ALPHABET_AND_DIGITS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new SecureRandom();

    public static String generateToken() {
        StringBuilder sb = new StringBuilder(20);
        for (int i = 0; i < 20; i++) {
            int randomIndex = RANDOM.nextInt(ALPHABET_AND_DIGITS.length());
            char randomChar = ALPHABET_AND_DIGITS.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }
}
