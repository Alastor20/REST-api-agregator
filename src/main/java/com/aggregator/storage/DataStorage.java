package com.aggregator.storage;

import com.aggregator.model.DataItem;
import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public interface DataStorage {
    static DataStorage of(Path file, OutputFormat format, ObjectMapper mapper) {
        if (file == null) {
            throw new IllegalArgumentException("Output file is required");
        }
        OutputFormat resolved = format != null ? format : detectByExtension(file);
        if (resolved == null) {
            throw new IllegalArgumentException("Cannot determine format for " + file + " (use --format or .csv/.json extension)");
        }
        return switch (resolved) {
            case CSV -> new CsvDataStorage(file, mapper);
            case JSON -> new JsonDataStorage(file, mapper);
        };
    }

    private static OutputFormat detectByExtension(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) return OutputFormat.CSV;
        if (name.endsWith(".json")) return OutputFormat.JSON;
        return null;
    }

    void save(List<DataItem> items, WriteMode mode) throws StorageException;

    List<DataItem> load() throws StorageException;
}