package com.lld.auth.redis.lua;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * 脚本管理器
 * **/
@Component
public class LuaScriptManager {

    private final RedisScript<List> cleanupScript; // 清理在线记录过时的lua脚本
    private final RedisScript<List> statsScript;  // 统计在线用户数的lua脚本
    private final RedisScript<List> loginCheckAndLimitScript; // 登录检查并限制登录的lua脚本
    private final RedisScript<List> loginScript; // 登录的lua脚本
    private final RedisScript<List> userHeartbeatScript; // 用户心跳的lua脚本
    private final LuaScriptLoader scriptLoader;


    @Autowired
    public LuaScriptManager(StringRedisTemplate stringRedisTemplate) {
        this.scriptLoader = new LuaScriptLoader(stringRedisTemplate);
        // fallbacksha 是脚本的sha值，可以在redis-cli中执行SHA1命令获取
        //sha值必须与脚本内容一致，脚本内容变动要重新生成

        // 清理在线记录过期的lua脚本
        this.cleanupScript = scriptLoader.loadScript("cleanup_online_users.lua",List.class ,
                "cleanup_online_users");
        // 统计在线用户数的lua脚本
        this.statsScript = scriptLoader.loadScript("online_statistics.lua",List.class ,
                "online_statistics");
        // 统计在线用户数的lua脚本
        this.loginCheckAndLimitScript = scriptLoader.loadScript("login_check_and_limit.lua",List.class ,
                "login_check_and_limit");
        this.loginScript = scriptLoader.loadScript("user_login.lua",List.class ,
                "user_login");
        this.userHeartbeatScript = scriptLoader.loadScript("user_heartbeat.lua",List.class ,
                "user_heartbeat");
    }

    public RedisScript<List> getCleanupScript() {
        return cleanupScript;
    }

    public RedisScript<List> getStatsScript() {
        return statsScript;
    }

    public RedisScript<List> getLoginCheckAndLimitScript() {
        return loginCheckAndLimitScript;
    }

    public RedisScript<List> getLoginScript() {
        return loginScript;
    }

    public RedisScript<List> getUserHeartbeatScript() {
        return userHeartbeatScript;
    }
}


