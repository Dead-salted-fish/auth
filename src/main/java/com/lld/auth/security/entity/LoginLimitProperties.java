package com.lld.auth.security.entity;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class LoginLimitProperties {

    /**
     * 用户登录最大失败次数
     */
    @Value("${security.login.limit.accountMaxFailures:3}")
    private int accountMaxFailures;

    /**
     * IP登录最大失败次数
     */
    @Value("${security.login.limit.ipMaxFailures:50}")
    private int ipMaxFailures;
    /**
     * 锁定时间（秒）
     */
    @Value("${security.login.limit.lockDuration:600}")
    private int lockDuration ;
    /**
     * 失败计数过期时间（秒）
     */
    @Value("${security.login.limit.countExpire:600}")
    private int countExpire;

    /**
    * 登录限制开关
     * true 开启  false 关闭 默认开启
     * 关闭则不会对登录失败做限制
    * */
    @Value("${security.login.openlimit:true}")
    private boolean openLimit;

}

