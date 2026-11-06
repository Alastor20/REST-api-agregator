package com.aggregator.cli;

import com.aggregator.aggregation.Aggregator;
import com.aggregator.api.ApiException;
import com.aggregator.model.DataItem;
import com.aggregator.storage.DataStorage;
import com.aggregator.storage.StorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliCommands {
    private static final Logger log = LoggerFactory.getLogger(CliCommands.class);

    private final Aggregator aggregator;
    private final ObjectMapper objectMapper;

    public CliCommands(Aggregator aggregator, ObjectMapper objectMapper) {
        this.aggregator = aggregator;
        this.objectMapper = objectMapper;
    }

    public void execute(CommandArgs args) throws Exception {
        if (args.file == null) {
            throw new IllegalArgumentException("--file is required");
        }
        DataStorage storage = DataStorage.of(args.file, args.format, objectMapper);

        switch (args.command) {
            case "poll" -> poll(args, storage);
            case "show" -> show(args, storage);
            default -> throw new IllegalArgumentException("Unknown command: " + args.command);
        }
    }

    public void poll(CommandArgs args, DataStorage storage) throws ApiException, StorageException {
        log.info("Command poll: apis={} format={} mode={} file={}", args.apis, args.format, args.mode, args.file);
        Map<String, Map<String, String>> overrides = new HashMap<>(args.queryOverrides);
        List<DataItem> items = aggregator.aggregate(args.apis, overrides);
        storage.save(items, args.mode);
        log.info("Saved {} item(s) to {} ({} / {})", items.size(), args.file, args.format, args.mode);
    }

    public void show(CommandArgs args, DataStorage storage) throws StorageException {
        log.info("Command show: file={} source={}", args.file, args.sourceFilter);

        List<DataItem> items = storage.load();
        List<DataItem> filtered = args.sourceFilter == null ? items : items.stream().filter(i -> args.sourceFilter.equals(i.source())).toList();

        if (filtered.isEmpty()) {
            log.info("(no records)");
            return;
        }
        for (DataItem item : filtered) {
            System.out.println(item);
        }
        log.info("Total: {} record(s)", filtered.size());
    }
}