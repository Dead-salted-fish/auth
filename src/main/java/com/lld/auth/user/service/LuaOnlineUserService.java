package com.lld.auth.user.service;

import com.lld.auth.user.entity.CleanupResult;
import com.lld.auth.user.entity.OnlineStatistics;

public interface LuaOnlineUserService {

    /**
     * 清理超时用户
     */
    CleanupResult cleanupTimeoutUsers();

    /**
     * 清理超时用户
     */
    CleanupResult cleanupTimeoutUsers(long timeoutMs, int maxProcess);
    /**
     * 获取在线统计信息
     */
     OnlineStatistics getOnlineStatistics();
}
