package com.lld.auth.utils;
/**
 * 常量类
 * */
public class AuthPublicConstantKeys {
    //记录在线用户的位图 key
    public static final String ONLINE_BITMAP_KEY = "user:online:bitmap";
    //记录在线用户的zset key
    public static final String ACTIVITY_ZSET_KEY = "user:activity:zset";
    //记录当日 登陆过的用户 位图的前缀
    public static final String USER_LOGGED_BITMAP_PREFIX = "user:logged:bitmap:";

    //用户连续登录失败被限制登录 前缀
    public static final String USER_LOGIN_FAIL_LOCK_PREFIX= "auth:login:fail:lock:";

    //用户连续登录失败次数 前缀
    public static final String USER_LOGIN_FAIL_COUNT_PREFIX= "auth:login:fail:count:";
    //IP连续登录失败被限制登录 前缀
    public static final String IP_LOGIN_FAIL_LOCK_PREFIX= "auth:login:fail:ip:lock:";
    //IP连续锁定账户次数
    public static final String IP_LOGIN_FAIL_COUNT_PREFIX= "auth:login:fail:ip:count:";
    private AuthPublicConstantKeys(){

    }
}
