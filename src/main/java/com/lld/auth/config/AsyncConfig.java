package com.lld.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    /**
     * 登录日志线程池
     */
    @Bean(name = "loginLogExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 核心线程数
        executor.setMaxPoolSize(10); // 最大线程数
        executor.setQueueCapacity(20); // 队列容量
        executor.setThreadNamePrefix("loginLog-Thread-"); // 线程名前缀
        executor.initialize();
        return executor;
    }
}
