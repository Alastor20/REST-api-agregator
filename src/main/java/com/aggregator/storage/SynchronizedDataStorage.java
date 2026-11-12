package com.aggregator.storage;

import com.aggregator.model.DataItem;
import com.aggregator.model.WriteMode;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedDataStorage implements DataStorage {

    private final DataStorage delegate;
    private final ReentrantLock lock = new ReentrantLock();

    public SynchronizedDataStorage(DataStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(List<DataItem> items, WriteMode mode) throws StorageException {
        lock.lock();
        try {
            delegate.save(items, mode);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<DataItem> load() throws StorageException {
        lock.lock();
        try {
            return delegate.load();
        } finally {
            lock.unlock();
        }
    }
}
