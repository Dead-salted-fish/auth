package com.lld.auth.Scheduler;

import com.lld.auth.user.entity.CleanupResult;
import com.lld.auth.user.entity.UserCleanupDetail;
import com.lld.auth.user.service.LuaOnlineUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;


@Component
public class OnlineUserCleanupScheduler {

    @Autowired
    private LuaOnlineUserService onlineUserService;

    private final LongAdder totalCleaned = new LongAdder();
    private final LongAdder totalTime = new LongAdder();

    private static final Logger logger = LoggerFactory.getLogger(OnlineUserCleanupScheduler.class);
    /**
     * 每30分钟执行一次清理
     */
    @Scheduled(fixedRate = 1800000)
    public void scheduledCleanup() {
        System.out.println("开始清理用户");
        long startTime = System.currentTimeMillis();

        try {
            CleanupResult result = onlineUserService.cleanupTimeoutUsers();

            long duration = System.currentTimeMillis() - startTime;
            totalCleaned.add(result.getTotalRemoved());
            totalTime.add(duration);

            List<UserCleanupDetail> details = result.getDetails();
            StringBuffer clearUser = new StringBuffer();
            for (UserCleanupDetail detail : details) {
                clearUser.append(detail.getUserId()).append(",");
            }
            String clearUserStr = "无清理用户";
            if(clearUser.length() > 1){
                 clearUserStr = clearUser.substring(0, clearUser.length()-1);
            }


            logger.warn("清理用户: {}", clearUserStr);

            logger.warn("清理完成: 总数={}, 耗时={}ms",
                    result.getTotalRemoved(),
                    duration);

            // 监控告警
            if (duration > 5000) {
                logger.warn("清理任务执行缓慢: {}ms", duration);
            }

            if (result.getTotalRemoved() > 1000) {
                logger.info("批量清理了大量用户: {}", result.getTotalRemoved());
            }

        } catch (Exception e) {
            logger.error("清理任务执行失败", e);
        }
    }

//    /**
//     * 每小时报告统计信息
//     */
//    @Scheduled(fixedRate = 3600000)
//    public void reportStatistics() {
//        long totalCleanedCount = totalCleaned.sum();
//        long totalTimeMs = totalTime.sum();
//
//        if (totalCleanedCount > 0) {
//            double avgTimePerUser = (double) totalTimeMs / totalCleanedCount;
//            log.info("清理统计: 总数={}, 总耗时={}ms, 平均={}ms/用户",
//                    totalCleanedCount, totalTimeMs, avgTimePerUser);
//        }
//
//        // 获取当前在线统计
//        LuaOnlineUserService.OnlineStatistics stats = onlineUserService.getOnlineStatistics();
//        log.info("当前在线统计: 总数={}, 活跃={}, 离开={}, 忙碌={}, 真实在线={}",
//                stats.getTotalOnline(), stats.getActiveOnline(),
//                stats.getAwayCount(), stats.getBusyCount(), stats.getRealOnline());
//    }
}
