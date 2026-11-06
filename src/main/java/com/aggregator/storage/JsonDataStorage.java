package com.aggregator.storage;

import com.aggregator.model.DataItem;
import com.aggregator.model.WriteMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonDataStorage implements DataStorage {
    private static final Logger log = LoggerFactory.getLogger(JsonDataStorage.class);

    private final Path path;
    private final ObjectMapper mapper;

    public JsonDataStorage(Path path, ObjectMapper mapper) {
        this.path = path;
        this.mapper = mapper;
    }

    @Override
    public void save(List<DataItem> items, WriteMode mode) throws StorageException {
        log.debug("JSON save: path={} mode={} items={}", path, mode, items.size());
        try {
            List<DataItem> all = new ArrayList<>();
            if (mode == WriteMode.APPEND && Files.exists(path) && Files.size(path) > 0) {
                all.addAll(readAll());
                log.debug("JSON save: appending to existing {} record(s)", all.size());
            }
            all.addAll(items);

            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);

            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(w, all);
            }
            log.info("JSON saved {} item(s) to {} (mode={})", items.size(), path, mode);
        } catch (IOException e) {
            log.error("JSON save failed: path={} mode={}", path, mode, e);
            throw new StorageException("JSON save failed", e);
        }
    }

    @Override
    public List<DataItem> load() throws StorageException {
        try {
            if (!Files.exists(path) || Files.size(path) == 0) {
                log.debug("JSON load: file missing or empty, returning empty list: {}", path);
                return List.of();
            }
            List<DataItem> items = readAll();
            log.info("JSON loaded {} item(s) from {}", items.size(), path);
            return items;
        } catch (IOException e) {
            log.error("JSON load failed: path={}", path, e);
            throw new StorageException("JSON load failed", e);
        }
    }

    private List<DataItem> readAll() throws IOException {
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return mapper.readValue(r, new TypeReference<List<DataItem>>() {
            });
        }
    }
}