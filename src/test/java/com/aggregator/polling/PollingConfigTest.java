package com.aggregator.polling;

import com.aggregator.config.AppProperties;
import com.aggregator.config.PollingConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PollingConfigTest {

    @Test
    void createsExecutorWithConfiguredPoolSizeAndPolicies() throws InterruptedException {
        AppProperties properties = new AppProperties();
        properties.getPolling().setN(2);

        ScheduledExecutorService executor = new PollingConfig().pollingExecutor(properties);
        try {
            assertThat(executor).isInstanceOf(ScheduledThreadPoolExecutor.class);
            ScheduledThreadPoolExecutor scheduled = (ScheduledThreadPoolExecutor) executor;
            assertThat(scheduled.getCorePoolSize()).isEqualTo(2);
            assertThat(scheduled.getRemoveOnCancelPolicy()).isTrue();
            assertThat(scheduled.getExecuteExistingDelayedTasksAfterShutdownPolicy()).isFalse();
            assertThat(scheduled.getContinueExistingPeriodicTasksAfterShutdownPolicy()).isFalse();

            AtomicReference<String> threadName = new AtomicReference<>();
            CountDownLatch ran = new CountDownLatch(1);
            executor.execute(() -> {
                threadName.set(Thread.currentThread().getName());
                ran.countDown();
            });
            assertThat(ran.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(threadName.get()).startsWith("polling-");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void enforcesMinimumPoolSizeOfOne() {
        AppProperties properties = new AppProperties();
        properties.getPolling().setN(0);

        ScheduledExecutorService executor = new PollingConfig().pollingExecutor(properties);
        try {
            assertThat(((ScheduledThreadPoolExecutor) executor).getCorePoolSize()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
