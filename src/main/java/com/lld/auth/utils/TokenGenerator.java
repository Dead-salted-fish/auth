package com.lld.auth.utils;

import java.security.SecureRandom;
import java.util.Random;

public class TokenGenerator {
    private static final byte[] ALPHABET_BYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".getBytes();
    private static final Random RANDOM = new SecureRandom();

    public static String generateToken() {
        byte[] tokenBytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            tokenBytes[i] = ALPHABET_BYTES[RANDOM.nextInt(62)];
        }
        return new String(tokenBytes);
    }
}
