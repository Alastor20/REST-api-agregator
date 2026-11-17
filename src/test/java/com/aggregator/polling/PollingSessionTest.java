package com.aggregator.polling;

import com.aggregator.model.DataItem;
import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;
import com.aggregator.storage.DataStorage;
import com.aggregator.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PollingSessionTest {

    private final DataStorage storage = mock(DataStorage.class);
    private final List<DataItem> items = List.of(
            new DataItem("weather", Instant.EPOCH, Map.of("city", "Berlin"), Map.of("temperature", 5)));

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveModeCases")
    void switchesToAppendOnlyAfterSuccessfulCreate(String scenario, WriteMode requestedMode,
                                                   boolean firstSaveFails, List<WriteMode> expectedModes)
            throws StorageException {
        if (firstSaveFails) {
            doThrow(new StorageException("first")).doNothing()
                    .when(storage).save(anyList(), eq(WriteMode.CREATE));
        }
        PollingSession session = session(requestedMode);

        if (firstSaveFails) {
            assertThatThrownBy(() -> session.save(items)).isInstanceOf(StorageException.class);
            session.save(items);
        } else {
            session.save(items);
            session.save(items);
        }

        InOrder ordered = inOrder(storage);
        for (WriteMode mode : expectedModes) {
            ordered.verify(storage, calls(1)).save(items, mode);
        }
        ordered.verifyNoMoreInteractions();
    }

    @Test
    void executeDoesNothingAfterStop() {
        PollingSession session = session(WriteMode.CREATE);
        AtomicBoolean executed = new AtomicBoolean();

        session.stop();
        session.stop();
        session.execute(() -> executed.set(true));

        assertThat(executed).isFalse();
        assertThat(session.isRunning()).isFalse();
    }

    @Test
    void stopWaitsForInFlightTask() throws InterruptedException {
        PollingSession session = session(WriteMode.CREATE);
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);
        AtomicBoolean taskFinished = new AtomicBoolean();

        Thread worker = new Thread(() -> session.execute(() -> {
            taskStarted.countDown();
            try {
                releaseTask.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            taskFinished.set(true);
        }));
        worker.start();
        assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();

        Thread stopper = new Thread(session::stop);
        stopper.start();
        await().atMost(5, TimeUnit.SECONDS).until(() -> stopper.getState() == Thread.State.WAITING);
        assertThat(taskFinished).isFalse();

        releaseTask.countDown();
        stopper.join(5_000);
        worker.join(5_000);

        assertThat(taskFinished).isTrue();
        assertThat(session.isRunning()).isFalse();
    }

    @Test
    void cancelsScheduledFuturesOnStopAndRejectsLateOnes() {
        ScheduledFuture<?> registered = mock(ScheduledFuture.class);
        ScheduledFuture<?> late = mock(ScheduledFuture.class);
        PollingSession session = session(WriteMode.CREATE);

        session.addFuture(registered);
        session.stop();
        session.addFuture(late);

        verify(registered).cancel(false);
        verify(late).cancel(false);
    }

    @Test
    void exposesSessionMetadata() {
        Instant startedAt = Instant.parse("2026-01-02T03:04:05Z");
        Map<String, List<Map<String, String>>> sources = Map.of("weather", List.of(Map.of("city", "Berlin")));
        Path file = Path.of("data", "results.json");
        PollingSession session = new PollingSession(sources, storage, WriteMode.CREATE,
                OutputFormat.JSON, file, 2, 7, startedAt);

        assertThat(session.getSources()).isEqualTo(sources);
        assertThat(session.getStorage()).isSameAs(storage);
        assertThat(session.getRequestedMode()).isEqualTo(WriteMode.CREATE);
        assertThat(session.getFormat()).isEqualTo(OutputFormat.JSON);
        assertThat(session.getFile()).isEqualTo(file);
        assertThat(session.getN()).isEqualTo(2);
        assertThat(session.getT()).isEqualTo(7);
        assertThat(session.getStartedAt()).isEqualTo(startedAt);
        assertThat(session.isRunning()).isTrue();
    }

    private static Stream<Arguments> saveModeCases() {
        return Stream.of(
                Arguments.of("create then append", WriteMode.CREATE, false,
                        List.of(WriteMode.CREATE, WriteMode.APPEND)),
                Arguments.of("requested append mode always appends", WriteMode.APPEND, false,
                        List.of(WriteMode.APPEND, WriteMode.APPEND)),
                Arguments.of("failed create stays create", WriteMode.CREATE, true,
                        List.of(WriteMode.CREATE, WriteMode.CREATE))
        );
    }

    private PollingSession session(WriteMode mode) {
        return new PollingSession(Map.of("weather", List.of(Map.of("city", "Berlin"))), storage, mode,
                OutputFormat.JSON, Path.of("data", "results.json"), 2, 5, Instant.EPOCH);
    }
}
