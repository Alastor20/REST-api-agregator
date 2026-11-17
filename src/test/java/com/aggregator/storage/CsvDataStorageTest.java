package com.aggregator.storage;

import com.aggregator.model.DataItem;
import com.aggregator.model.WriteMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvDataStorageTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveCases")
    void savesItemsAccordingToMode(String scenario, List<DataItem> existing, WriteMode mode,
                                   List<DataItem> items, List<DataItem> expected)
            throws StorageException, IOException {
        Path file = tempDir.resolve("data.csv");
        CsvDataStorage storage = new CsvDataStorage(file, mapper);
        if (existing != null) {
            storage.save(existing, WriteMode.CREATE);
        }

        storage.save(items, mode);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertThat(file).exists();
        assertThat(storage.load()).containsExactlyElementsOf(expected);
        assertThat(lines.get(0)).isEqualTo("source,timestamp,query,data");
        assertThat(lines.stream().filter(line -> line.startsWith("source,timestamp,query,data")).count())
                .isEqualTo(1);
    }

    @Test
    void returnsEmptyListForMissingOrEmptyFile() throws StorageException, IOException {
        CsvDataStorage missing = new CsvDataStorage(tempDir.resolve("missing.csv"), mapper);
        assertThat(missing.load()).isEmpty();

        Path empty = tempDir.resolve("empty.csv");
        Files.createFile(empty);
        assertThat(new CsvDataStorage(empty, mapper).load()).isEmpty();
    }

    @Test
    void wrapsSaveFailures() throws IOException {
        Path blocker = tempDir.resolve("blocker");
        Files.writeString(blocker, "not a directory");
        CsvDataStorage storage = new CsvDataStorage(blocker.resolve("data.csv"), mapper);

        assertThatThrownBy(() -> storage.save(items(), WriteMode.CREATE))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("CSV save failed");
    }

    @Test
    void wrapsLoadFailures() throws IOException {
        Path file = tempDir.resolve("broken.csv");
        Files.write(file, new byte[]{(byte) 0xC3, (byte) 0x28});
        CsvDataStorage storage = new CsvDataStorage(file, mapper);

        assertThatThrownBy(storage::load)
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("CSV load failed");
    }

    private static Stream<Arguments> saveCases() {
        DataItem first = item("weather", "1");
        DataItem second = item("currency", "2");
        List<DataItem> pair = items();
        return Stream.of(
                Arguments.of("create writes new file", null, WriteMode.CREATE, pair, pair),
                Arguments.of("append keeps a single header", List.of(first), WriteMode.APPEND,
                        List.of(second), List.of(first, second)),
                Arguments.of("append to missing file writes header", null, WriteMode.APPEND,
                        pair, pair)
        );
    }

    private static List<DataItem> items() {
        return List.of(item("weather", "1"), item("books", "2"));
    }

    private static DataItem item(String source, String value) {
        return new DataItem(source, Instant.parse("2026-01-02T03:04:05Z"),
                Map.of("value", value), Map.of("nested", Map.of("x", 1)));
    }
}
