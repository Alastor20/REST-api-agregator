package com.aggregator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class PollingConfig {

    @Bean(name = "pollingExecutor", destroyMethod = "")
    public ScheduledExecutorService pollingExecutor(AppProperties properties) {
        int poolSize = Math.max(1, properties.getPolling().getN());
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(poolSize, pollingThreadFactory());
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return executor;
    }

    private ThreadFactory pollingThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "polling-" + counter.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }
}
