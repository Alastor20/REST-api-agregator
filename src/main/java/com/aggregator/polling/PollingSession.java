package com.aggregator.polling;

import com.aggregator.model.DataItem;
import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;
import com.aggregator.storage.DataStorage;
import com.aggregator.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PollingSession {

    private static final Logger log = LoggerFactory.getLogger(PollingSession.class);

    private final Map<String, List<Map<String, String>>> sources;
    private final DataStorage storage;
    private final WriteMode requestedMode;
    private final OutputFormat format;
    private final Path file;
    private final int n;
    private final long t;
    private final Instant startedAt;

    private final List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<>();
    private final ReentrantReadWriteLock executionLock = new ReentrantReadWriteLock();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private volatile boolean stopping;
    private boolean firstSave = true;

    public PollingSession(Map<String, List<Map<String, String>>> sources, DataStorage storage, WriteMode requestedMode, OutputFormat format, Path file, int n, long t, Instant startedAt) {
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
        this.storage = storage;
        this.requestedMode = requestedMode;
        this.format = format;
        this.file = file;
        this.n = n;
        this.t = t;
        this.startedAt = startedAt;
    }

    public void addFuture(ScheduledFuture<?> future) {
        if (stopping) {
            future.cancel(false);
            return;
        }
        futures.add(future);
    }

    public void execute(Runnable task) {
        executionLock.readLock().lock();
        try {
            if (stopping) {
                return;
            }
            task.run();
        } finally {
            executionLock.readLock().unlock();
        }
    }

    public synchronized void save(List<DataItem> items) throws StorageException {
        WriteMode mode;
        if (requestedMode == WriteMode.APPEND) {
            mode = WriteMode.APPEND;
        } else {
            mode = firstSave ? WriteMode.CREATE : WriteMode.APPEND;
        }
        storage.save(items, mode);
        firstSave = false;
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        stopping = true;
        for (ScheduledFuture<?> future : futures) {
            future.cancel(false);
        }
        futures.clear();

        executionLock.writeLock().lock();
        try {
            log.info("Polling session stopped, all in-flight tasks completed");
        } finally {
            executionLock.writeLock().unlock();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public Map<String, List<Map<String, String>>> getSources() {
        return sources;
    }

    public DataStorage getStorage() {
        return storage;
    }

    public WriteMode getRequestedMode() {
        return requestedMode;
    }

    public OutputFormat getFormat() {
        return format;
    }

    public Path getFile() {
        return file;
    }

    public int getN() {
        return n;
    }

    public long getT() {
        return t;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
