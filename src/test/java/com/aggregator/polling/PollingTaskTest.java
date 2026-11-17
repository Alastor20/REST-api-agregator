package com.aggregator.polling;

import com.aggregator.api.ApiException;
import com.aggregator.api.ApiSource;
import com.aggregator.model.DataItem;
import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;
import com.aggregator.storage.DataStorage;
import com.aggregator.storage.StorageException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PollingTaskTest {

    private final ApiSource source = mock(ApiSource.class);
    private final DataStorage storage = mock(DataStorage.class);
    private final Map<String, String> params = Map.of("city", "Berlin");
    private final List<DataItem> items = List.of(
            new DataItem("weather", Instant.EPOCH, params, Map.of("temperature", 5)));

    @Test
    void fetchesAndSavesItems() throws ApiException, StorageException {
        when(source.fetch(params)).thenReturn(items);
        PollingTask task = new PollingTask(source, params, session());

        task.run();

        verify(source).fetch(params);
        verify(storage).save(items, WriteMode.CREATE);
    }

    @Test
    void swallowsApiFailures() throws ApiException {
        when(source.fetch(params)).thenThrow(new ApiException("boom"));
        PollingTask task = new PollingTask(source, params, session());

        task.run();

        verifyNoInteractions(storage);
    }

    @Test
    void swallowsStorageFailures() throws ApiException, StorageException {
        when(source.fetch(params)).thenReturn(items);
        doThrow(new StorageException("boom")).when(storage).save(anyList(), eq(WriteMode.CREATE));
        PollingTask task = new PollingTask(source, params, session());

        task.run();

        verify(storage).save(items, WriteMode.CREATE);
    }

    @Test
    void swallowsUnexpectedSourceFailures() throws ApiException, StorageException {
        when(source.fetch(params)).thenThrow(new IllegalStateException("boom"));
        PollingTask task = new PollingTask(source, params, session());

        task.run();

        verify(storage, never()).save(anyList(), eq(WriteMode.CREATE));
    }

    @Test
    void swallowsUnexpectedStorageFailures() throws ApiException, StorageException {
        when(source.fetch(params)).thenReturn(items);
        doThrow(new IllegalStateException("boom")).when(storage).save(anyList(), eq(WriteMode.CREATE));
        PollingTask task = new PollingTask(source, params, session());

        task.run();

        verify(storage).save(items, WriteMode.CREATE);
    }

    private PollingSession session() {
        return new PollingSession(Map.of("weather", List.of(params)), storage, WriteMode.CREATE,
                OutputFormat.JSON, Path.of("data", "results.json"), 2, 5, Instant.EPOCH);
    }
}
