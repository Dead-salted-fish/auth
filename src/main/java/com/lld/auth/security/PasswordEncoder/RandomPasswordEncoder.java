package com.lld.auth.security.PasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

public class RandomPasswordEncoder implements PasswordEncoder {

    private static final String PREFIX = "{";

    private static final String SUFFIX = "}";

    private final Map<String, PasswordEncoder> passwordEncoderMap;

    public RandomPasswordEncoder(Map<String, PasswordEncoder> passwordEncoderMap) {
        this.passwordEncoderMap = passwordEncoderMap;
    }

    @Override
    public String encode(CharSequence charSequence) {

        List<String> encoderTypes = new ArrayList<>();
        for (String type : passwordEncoderMap.keySet()) {
            encoderTypes.add(type);
        }

        Random random = new Random();
        int i = random.nextInt(encoderTypes.size());
        String encoderType = encoderTypes.get(i);
        String passWordEncoded = passwordEncoderMap.get(encoderTypes.get(i)).encode(charSequence);

        return PREFIX + encoderType + SUFFIX+passWordEncoded;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String prefixEncodedPassword) {
        if (rawPassword == null && prefixEncodedPassword == null) {
            return true;
        }
        String id = extractId(prefixEncodedPassword);
        PasswordEncoder passwordEncoder = this.passwordEncoderMap.get(id);
        if (passwordEncoder == null) {
            throw new RuntimeException("无对应的加密器,请联系管理员查明原因");
        }
        String encodedPassword = extractEncodedPassword(prefixEncodedPassword);
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private String extractId(String prefixEncodedPassword) {
        if (prefixEncodedPassword == null) {
            return null;
        }
        int start = prefixEncodedPassword.indexOf(PREFIX);
        if (start != 0) {
            return null;
        }
        int end = prefixEncodedPassword.indexOf(SUFFIX, start);
        if (end < 0) {
            return null;
        }
        return prefixEncodedPassword.substring(start + 1, end);
    }

    @Override
    public boolean upgradeEncoding(String prefixEncodedPassword) {
        String id = extractId(prefixEncodedPassword);
        String encodedPassword = extractEncodedPassword(prefixEncodedPassword);
        return this.passwordEncoderMap.get(id).upgradeEncoding(encodedPassword);
    }

    private String extractEncodedPassword(String prefixEncodedPassword) {
        int start = prefixEncodedPassword.indexOf(SUFFIX);
        return prefixEncodedPassword.substring(start + 1);
    }

}
