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

       // 现在固定使用 bcrypt 加密器
        String encoderType = "bcrypt";
        String passWordEncoded = passwordEncoderMap.get(encoderType).encode(charSequence);

        return PREFIX + encoderType + SUFFIX+passWordEncoded;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String prefixEncodedPassword) {

        if (rawPassword == null && prefixEncodedPassword == null) {
            return true;
        }
        // 获取加密器类型
        // 为了和历史版本兼容，以前是随机加密方式，现在只有 bcrypt 加密器
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

    // 密码平滑升级
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
