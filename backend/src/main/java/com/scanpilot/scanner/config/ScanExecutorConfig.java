package com.scanpilot.scanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bounded In-Process Scan Executor Configuration (FR-002, NFR-001, Issue #52).
 * Strictly bounded execution pool (1 worker thread, max capacity 10 in queue)
 * with AbortPolicy rejection to protect node stability.
 */
@Configuration
public class ScanExecutorConfig {

    public static final int MAX_QUEUED_JOBS_PER_REPOSITORY = 10;

    @Bean(name = "scanTaskExecutor")
    public ThreadPoolTaskExecutor scanTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("scan-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
