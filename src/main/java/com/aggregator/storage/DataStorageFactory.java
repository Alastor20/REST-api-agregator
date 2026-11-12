package com.aggregator.storage;

import com.aggregator.model.OutputFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class DataStorageFactory {

    private final ObjectMapper objectMapper;

    public DataStorageFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DataStorage create(Path file, OutputFormat format) {
        return new SynchronizedDataStorage(DataStorage.of(file, format, objectMapper));
    }
}
