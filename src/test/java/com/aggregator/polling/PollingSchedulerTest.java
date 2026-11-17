package com.aggregator.polling;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PollingSchedulerTest {

    private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    private final PollingScheduler scheduler = new PollingScheduler(executor);

    @Test
    void schedulesWithFixedDelayStartingImmediately() {
        Runnable task = () -> {
        };
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(executor).scheduleWithFixedDelay(task, 0, 5, TimeUnit.SECONDS);

        assertThat((Object) scheduler.scheduleWithFixedDelay(task, 5)).isSameAs(future);
    }

    @Test
    void cancelsFuturesWithoutInterrupting() {
        ScheduledFuture<?> first = mock(ScheduledFuture.class);
        ScheduledFuture<?> second = mock(ScheduledFuture.class);

        scheduler.cancel(List.of(first, second));

        verify(first).cancel(false);
        verify(second).cancel(false);
    }

    @Test
    void shutsDownGracefully() throws InterruptedException {
        when(executor.awaitTermination(30, TimeUnit.SECONDS)).thenReturn(true);

        scheduler.shutdown();

        verify(executor).shutdown();
        verify(executor).awaitTermination(30, TimeUnit.SECONDS);
        verify(executor, never()).shutdownNow();
    }

    @Test
    void forcesShutdownWhenTerminationTimesOut() throws InterruptedException {
        when(executor.awaitTermination(30, TimeUnit.SECONDS)).thenReturn(false);

        scheduler.shutdown();

        verify(executor).shutdownNow();
    }

    @Test
    void restoresInterruptAndForcesShutdown() throws InterruptedException {
        when(executor.awaitTermination(30, TimeUnit.SECONDS)).thenThrow(new InterruptedException("stop"));

        try {
            scheduler.shutdown();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(executor).shutdownNow();
        } finally {
            Thread.interrupted();
        }
    }
}
