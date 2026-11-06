package com.aggregator.storage;

import com.aggregator.model.DataItem;
import com.aggregator.model.WriteMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvDataStorage implements DataStorage {
    private static final Logger log = LoggerFactory.getLogger(CsvDataStorage.class);
    private static final String[] HEADER = {"source", "timestamp", "query", "data"};

    private final Path path;
    private final ObjectMapper mapper;

    public CsvDataStorage(Path path, ObjectMapper mapper) {
        this.path = path;
        this.mapper = mapper;
    }

    @Override
    public void save(List<DataItem> items, WriteMode mode) throws StorageException {
        log.debug("CSV save: path={} mode={} items={}", path, mode, items.size());
        try {
            boolean exists = Files.exists(path);
            boolean writeHeader = (mode == WriteMode.CREATE || !exists);

            Path parent = path.toAbsolutePath().getParent();
            if (writeHeader && parent != null) Files.createDirectories(parent);

            OpenOption[] opts = writeHeader ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE} : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(path, opts), StandardCharsets.UTF_8); CSVPrinter printer = new CSVPrinter(w, CSVFormat.DEFAULT)) {

                if (writeHeader) printer.printRecord((Object[]) HEADER);
                for (DataItem item : items) {
                    printer.printRecord(item.source(), item.timestamp().toString(), mapper.writeValueAsString(item.queryParams()), mapper.writeValueAsString(item.data()));
                }
            }
            log.info("CSV saved {} item(s) to {} (mode={}, header={})", items.size(), path, mode, writeHeader);
        } catch (IOException e) {
            log.error("CSV save failed: path={} mode={}", path, mode, e);
            throw new StorageException("CSV save failed", e);
        }
    }

    @Override
    public List<DataItem> load() throws StorageException {
        try {
            if (!Files.exists(path) || Files.size(path) == 0) {
                log.debug("CSV load: file missing or empty, returning empty list: {}", path);
                return List.of();
            }
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
                try (CSVParser parser = format.parse(r)) {
                    List<DataItem> items = new ArrayList<>();
                    for (CSVRecord record : parser) {
                        String source = record.get("source");
                        Instant ts = Instant.parse(record.get("timestamp"));
                        Map<String, String> qm = mapper.readValue(record.get("query"), new TypeReference<Map<String, String>>() {
                        });
                        Map<String, Object> dm = mapper.readValue(record.get("data"), new TypeReference<Map<String, Object>>() {
                        });
                        items.add(new DataItem(source, ts, qm, dm));
                    }
                    log.info("CSV loaded {} item(s) from {}", items.size(), path);
                    return items;
                }
            }
        } catch (IOException e) {
            log.error("CSV load failed: path={}", path, e);
            throw new StorageException("CSV load failed", e);
        }
    }
}