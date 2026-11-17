package com.aggregator.storage;

import com.aggregator.model.DataItem;
import com.aggregator.model.WriteMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SynchronizedDataStorageTest {

    @Test
    void delegatesSaveAndLoad() throws StorageException {
        DataStorage delegate = mock(DataStorage.class);
        List<DataItem> items = List.of();
        when(delegate.load()).thenReturn(items);
        SynchronizedDataStorage storage = new SynchronizedDataStorage(delegate);

        storage.save(items, WriteMode.APPEND);

        verify(delegate).save(items, WriteMode.APPEND);
        assertThat(storage.load()).isSameAs(items);
        verify(delegate).load();
    }

    @Test
    void propagatesDelegateFailures() throws StorageException {
        DataStorage delegate = mock(DataStorage.class);
        StorageException failure = new StorageException("boom");
        org.mockito.Mockito.doThrow(failure).when(delegate).save(List.of(), WriteMode.CREATE);
        SynchronizedDataStorage storage = new SynchronizedDataStorage(delegate);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> storage.save(List.of(), WriteMode.CREATE))
                .isSameAs(failure);
    }
}
