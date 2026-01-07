package com.lld.auth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 登录日志工具类
 **/
@Service
public class LoginLogUtil {

    private static final Logger loginlogger = LoggerFactory.getLogger("loginLog");

    private static final Logger normallogger = LoggerFactory.getLogger("normalLog");

    public LoginLogUtil() {

    }

    public void logUserLoginAsync(Long userID, String username, String ipAddress) {

        loginlogger.info("用ID {},用户 {} 从IP: {} 登陆", userID, username, ipAddress);
        // 或者使用其他方式将登录信息持久化到数据库、日志文件等
    }

    public void logFailureLoginAsync(String username, String ipAddress, String failureReason) {

        loginlogger.info("用户 {}， 从IP: {} 登录失败，失败原因: {}。", username, ipAddress, failureReason);
        // 或者使用其他方式将登出信息持久化到数据库、日志文件等
    }

    public void nornalLogInfo(String format, Object... arguments) {
        normallogger.info(format, arguments);
    }

    public void nornalLogWarn(String format, Object... arguments) {
        normallogger.warn(format, arguments);
    }

    public void nornalLogError(String format, Object... arguments) {
        normallogger.error(format, arguments);
    }


}
