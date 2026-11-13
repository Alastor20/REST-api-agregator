package com.aggregator.polling;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class PollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PollingScheduler.class);

    private final ScheduledExecutorService executor;

    public PollingScheduler(@Qualifier("pollingExecutor") ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long intervalSeconds) {
        return executor.scheduleWithFixedDelay(task, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void cancel(Collection<ScheduledFuture<?>> futures) {
        for (ScheduledFuture<?> future : futures) {
            future.cancel(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down polling executor");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Polling executor did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
