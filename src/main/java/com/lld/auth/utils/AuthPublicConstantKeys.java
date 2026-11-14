package com.lld.auth.utils;
/**
 * 常量类
 * */
public class AuthPublicConstantKeys {
    //记录在线用户的位图 key
    public static final String ONLINE_USERS_KEY = "user:online";
    /**
     * @deprecated 活跃时间有序集合已停用（登录 Lua 中 ZADD 已注释），
     *             仅为保持 user_login.lua / user_heartbeat.lua 的 KEYS 顺序占位，暂勿删除。
     */
    @Deprecated
    public static final String ACTIVITY_ZSET_KEY = "user:activity:zset";

    /** 当日活跃用户（DAU）前缀，按天分片；完整 key 形如 user:active:daily:2026-11-07 */
    public static final String DAILY_ACTIVE_USERS_PREFIX = "user:active:daily:";

    /** 账户锁定标记前缀，完整 key = prefix + username */
    public static final String ACCOUNT_LOGIN_FAIL_LOCK_PREFIX = "auth:login:fail:account:lock:";

    /** 账户登录失败次数前缀，完整 key = prefix + username */
    public static final String ACCOUNT_LOGIN_FAIL_COUNT_PREFIX = "auth:login:fail:account:count:";
    /** IP 锁定标记前缀，完整 key = prefix + clientIp */
    public static final String IP_LOGIN_FAIL_LOCK_PREFIX = "auth:login:fail:ip:lock:";
    /** IP 登录失败次数前缀，完整 key = prefix + clientIp（每次失败累加） */
    public static final String IP_LOGIN_FAIL_COUNT_PREFIX = "auth:login:fail:ip:count:";
    private AuthPublicConstantKeys(){

    }
}
