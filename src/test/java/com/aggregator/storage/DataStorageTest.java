package com.aggregator.storage;

import com.aggregator.model.OutputFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataStorageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void detectsFormatFromExtension() {
        assertThat(DataStorage.of(tempDir.resolve("a.csv"), null, mapper)).isInstanceOf(CsvDataStorage.class);
        assertThat(DataStorage.of(tempDir.resolve("a.JSON"), null, mapper)).isInstanceOf(JsonDataStorage.class);
    }

    @Test
    void explicitFormatWinsOverExtension() {
        assertThat(DataStorage.of(tempDir.resolve("a.json"), OutputFormat.CSV, mapper)).isInstanceOf(CsvDataStorage.class);
    }

    @Test
    void rejectsNullFileAndUnknownExtension() {
        assertThatThrownBy(() -> DataStorage.of(null, OutputFormat.JSON, mapper)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DataStorage.of(tempDir.resolve("a.txt"), null, mapper)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cannot determine format");
    }

    @Test
    void factoryWrapsStorageWithSynchronization() {
        DataStorage storage = new DataStorageFactory(mapper).create(tempDir.resolve("a.json"), OutputFormat.JSON);

        assertThat(storage).isInstanceOf(SynchronizedDataStorage.class);
    }
}
