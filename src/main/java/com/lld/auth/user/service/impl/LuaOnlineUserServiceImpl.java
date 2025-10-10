package com.lld.auth.user.service.impl;

import com.lld.auth.redis.lua.LuaScriptManager;
import com.lld.auth.user.entity.CleanupResult;
import com.lld.auth.user.entity.OnlineStatistics;
import com.lld.auth.user.entity.UserCleanupDetail;
import com.lld.auth.user.service.LuaOnlineUserService;
import com.lld.auth.utils.AuthPublicConstantKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LuaOnlineUserServiceImpl implements LuaOnlineUserService {
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000; // 30分钟
    private static final int MAX_PROCESS_COUNT = 10000; // 每次最多处理1万个用户
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);


    private final LuaScriptManager luaScriptManager;
    private final StringRedisTemplate stringRedisTemplate; // 添加StringRedisTemplate
    public LuaOnlineUserServiceImpl(LuaScriptManager luaScriptManager, StringRedisTemplate stringRedisTemplate) {
        this.luaScriptManager = luaScriptManager;
        this.stringRedisTemplate = stringRedisTemplate;

    }

    /**
     * 批量清理超时用户
     */
    public CleanupResult cleanupTimeoutUsers() {
        return cleanupTimeoutUsers(DEFAULT_TIMEOUT, MAX_PROCESS_COUNT);
    }

    /**
     * 带参数的清理方法
     */
    public CleanupResult cleanupTimeoutUsers(long timeoutMs, int maxProcess) {
        long currentTime = System.currentTimeMillis();

        List<String> keys = Arrays.asList(
                AuthPublicConstantKeys.ONLINE_BITMAP_KEY,
                AuthPublicConstantKeys.ACTIVITY_ZSET_KEY
        );

        // 修改 ARGV 为 List<String> 类型
        String[] argv = {
                String.valueOf(currentTime),
                String.valueOf(timeoutMs),
                String.valueOf(maxProcess)
        };

        RedisScript<List> cleanupScript = luaScriptManager.getCleanupScript();



        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) stringRedisTemplate.execute(
                cleanupScript,
                keys,argv
        );

        return parseCleanupResult(result);
    }

    /**
     * 获取在线统计信息
     */
    public OnlineStatistics getOnlineStatistics() {
        String todayStr = LocalDate.now().format(DATE_FORMATTER);
        List<String> keys = Arrays.asList(
                AuthPublicConstantKeys.ONLINE_BITMAP_KEY,
                 AuthPublicConstantKeys.TODAY_ONLINE_BITMAP_PREFIX+todayStr

        );

        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) stringRedisTemplate.execute(
                luaScriptManager.getStatsScript(),
                keys, new String[]{}
        );

        return parseStatisticsResult(result);
    }
   /**
    * 解析统计结果
    * **/
    private OnlineStatistics parseStatisticsResult(List<Object> result) {
        if (result == null || result.size() < 3) {
            return new OnlineStatistics(0, 0,0);
        }

        return new OnlineStatistics(
                ((Number) result.get(0)).longValue(),
                ((Number) result.get(1)).longValue(),
                ((Number) result.get(2)).longValue()
        );
    }

    // 解析清理结果
    private CleanupResult parseCleanupResult(List<Object> result) {
        if (result == null || result.size() < 2) {
            return new CleanupResult(0,  Collections.emptyList());
        }

        long totalRemoved = ((Number) result.get(0)).longValue();


        @SuppressWarnings("unchecked")
        List<List<Object>> detailedStats = (List<List<Object>>) result.get(1);

        List<UserCleanupDetail> details = detailedStats.stream()
                .map(stats -> new UserCleanupDetail(
                        Long.parseLong((String) stats.get(0))))
                .collect(Collectors.toList());

        return new CleanupResult(totalRemoved, details);
    }
}
