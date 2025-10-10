package com.lld.auth.utils;
/**
 * 常量类
 * */
public class AuthPublicConstantKeys {
    //记录在线用户的位图 key
    public static final String ONLINE_BITMAP_KEY = "online_users_bitmap";
    //记录在线用户的zset key
    public static final String ACTIVITY_ZSET_KEY = "user_activity_zset";
    //记录今日在线用户位图的后缀
    public static final String TODAY_ONLINE_BITMAP_PREFIX = "today_user_online_bitmap_";

    //用户连续登录失败被限制登录 前缀
    public static final String USER_LOGIN_FAIL_LOCK_PREFIX= "user_login_fail_lock_";

    //用户连续登录失败次数 前缀
    public static final String USER_LOGIN_FAIL_COUNT_PREFIX= "user_login_fail_count_";
    private AuthPublicConstantKeys(){

    }
}
