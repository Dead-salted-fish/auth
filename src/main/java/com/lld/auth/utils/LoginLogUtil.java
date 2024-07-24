package com.lld.auth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class LoginLogUtil {

    private static final Logger logger = LoggerFactory.getLogger(LoginLogUtil.class);

    private final ThreadPoolTaskExecutor loginLogExecutor;

    public LoginLogUtil(@Qualifier("loginLogExecutor") ThreadPoolTaskExecutor loginLogExecutor) {
        this.loginLogExecutor = loginLogExecutor;
    }

    public void logUserLoginAsync(Long userID,String username, String ipAddress) {
        loginLogExecutor.execute(() -> {
            System.out.println(userID);
            logger.info("用ID {},用户 {} 从IP: {} 登陆",userID, username, ipAddress);
            // 或者使用其他方式将登录信息持久化到数据库、日志文件等
        });
    }
}
