package com.lld.auth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 登录日志工具类
 **/
@Service
public class LogUtil {

    public static final Logger loginlogger = LoggerFactory.getLogger("loginLog");

    public static final Logger normallogger = LoggerFactory.getLogger("normalLog");

}
