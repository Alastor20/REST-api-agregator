package com.aggregator.polling;

import com.aggregator.api.ApiSource;
import com.aggregator.api.ApiSourceRegistry;
import com.aggregator.config.AppProperties;
import com.aggregator.model.DataItem;
import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;
import com.aggregator.storage.DataStorage;
import com.aggregator.storage.DataStorageFactory;
import com.aggregator.storage.StorageException;
import com.aggregator.web.dto.PollingRequestResponse;
import com.aggregator.web.dto.PollingStatusResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final ApiSourceRegistry registry;
    private final PollingScheduler scheduler;
    private final DataStorageFactory storageFactory;
    private final AppProperties properties;
    private final ReentrantLock stateLock = new ReentrantLock();

    private volatile PollingSession current;

    public PollingService(ApiSourceRegistry registry, PollingScheduler scheduler, DataStorageFactory storageFactory, AppProperties properties) {
        this.registry = registry;
        this.scheduler = scheduler;
        this.storageFactory = storageFactory;
        this.properties = properties;
    }

    public void start(PollingRequestResponse request) {
        stateLock.lock();
        try {
            if (current != null && current.isRunning()) {
                throw new PollingConflictException("Polling is already running");
            }

            int n = properties.getPolling().getN();
            long t = properties.getPolling().getT();
            if (n < 1) {
                throw new InvalidRequestException("app.polling.n must be greater than 0");
            }
            if (t < 1) {
                throw new InvalidRequestException("app.polling.t must be greater than 0 seconds");
            }

            OutputFormat format = resolveFormat(request == null ? null : request.format());
            WriteMode mode = resolveMode(request == null ? null : request.mode());
            Path file = resolveFile(request == null ? null : request.file());
            Map<String, List<Map<String, String>>> sources = resolveSources(request == null ? null : request.sources());

            DataStorage storage = storageFactory.create(file, format);
            PollingSession session = new PollingSession(sources, storage, mode, format, file, n, t, Instant.now());

            try {
                for (Map.Entry<String, List<Map<String, String>>> entry : sources.entrySet()) {
                    ApiSource source = registry.get(entry.getKey());
                    for (Map<String, String> params : entry.getValue()) {
                        PollingTask task = new PollingTask(source, params, session);
                        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(task, t);
                        session.addFuture(future);
                    }
                }
            } catch (RuntimeException e) {
                session.stop();
                throw e;
            }

            current = session;
            log.info("Polling started: sources={} format={} mode={} file={} n={} t={}", sources.keySet(), format, mode, file, n, t);
        } finally {
            stateLock.unlock();
        }
    }

    public void stop() {
        stateLock.lock();
        try {
            PollingSession session = current;
            if (session == null || !session.isRunning()) {
                throw new PollingConflictException("Polling is not running");
            }
            session.stop();
            log.info("Polling stopped");
        } finally {
            stateLock.unlock();
        }
    }

    public PollingStatusResponse status() {
        PollingSession session = current;
        if (session != null) {
            boolean running = session.isRunning();
            return new PollingStatusResponse(running, running ? session.getSources() : Map.of(), session.getN(), session.getT(), running ? session.getStartedAt().toString() : null, session.getFormat().name().toLowerCase(), session.getFile().toString(), session.getRequestedMode().name().toLowerCase());
        }
        return new PollingStatusResponse(false, Map.of(), properties.getPolling().getN(), properties.getPolling().getT(), null, properties.getStorage().getFormat(), properties.getStorage().getFile(), properties.getStorage().getMode());
    }

    public List<DataItem> loadData(String sourceFilter) throws StorageException {
        PollingSession session = current;
        DataStorage storage;
        if (session != null) {
            storage = session.getStorage();
        } else {
            OutputFormat format = resolveFormat(null);
            Path file = resolveFile(null);
            storage = storageFactory.create(file, format);
        }

        List<DataItem> items = storage.load();
        if (sourceFilter != null && !sourceFilter.isBlank()) {
            String filter = sourceFilter.trim();
            items = items.stream().filter(item -> filter.equals(item.source())).toList();
        }
        return items;
    }

    @PreDestroy
    public void shutdown() {
        stateLock.lock();
        try {
            PollingSession session = current;
            if (session != null && session.isRunning()) {
                log.info("Stopping polling during application shutdown");
                session.stop();
            }
        } finally {
            stateLock.unlock();
        }
    }

    private Map<String, List<Map<String, String>>> resolveSources(List<PollingRequestResponse.SourceRequest> requested) {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

        if (requested == null || requested.isEmpty()) {
            for (String name : registry.names()) {
                List<Map<String, String>> params = paramsFromConfig(name);
                if (params.isEmpty()) {
                    throw new InvalidRequestException("No parameter sets configured for source: " + name);
                }
                result.put(name, params);
            }
            return result;
        }

        for (PollingRequestResponse.SourceRequest sourceRequest : requested) {
            if (sourceRequest == null || sourceRequest.name() == null || sourceRequest.name().isBlank()) {
                throw new InvalidRequestException("Source name is required");
            }
            String name = sourceRequest.name().trim();
            if (result.containsKey(name)) {
                throw new InvalidRequestException("Duplicate source: " + name);
            }
            if (registry.get(name) == null) {
                throw new InvalidRequestException("Unknown API source: " + name);
            }

            List<Map<String, String>> params;
            if (sourceRequest.params() != null && !sourceRequest.params().isEmpty()) {
                params = validateParams(name, sourceRequest.params());
            } else {
                params = paramsFromConfig(name);
            }
            if (params.isEmpty()) {
                throw new InvalidRequestException("No parameter sets for source: " + name);
            }
            result.put(name, params);
        }
        return result;
    }

    private List<Map<String, String>> paramsFromConfig(String name) {
        AppProperties.Source source = properties.getSources().get(name);
        if (source == null || source.getParams() == null) {
            return List.of();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> params : source.getParams()) {
            if (params != null && !params.isEmpty()) {
                result.add(new LinkedHashMap<>(params));
            }
        }
        return result;
    }

    private List<Map<String, String>> validateParams(String name, List<Map<String, String>> params) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> paramSet : params) {
            if (paramSet == null || paramSet.isEmpty()) {
                throw new InvalidRequestException("Empty parameter set for source: " + name);
            }
            result.add(new LinkedHashMap<>(paramSet));
        }
        return result;
    }

    private OutputFormat resolveFormat(String requested) {
        String value = requested != null && !requested.isBlank() ? requested : properties.getStorage().getFormat();
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Output format is required");
        }
        try {
            return OutputFormat.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid format: " + value);
        }
    }

    private WriteMode resolveMode(String requested) {
        String value = requested != null && !requested.isBlank() ? requested : properties.getStorage().getMode();
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Write mode is required");
        }
        try {
            return WriteMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid mode: " + value);
        }
    }

    private Path resolveFile(String requested) {
        String value = requested != null && !requested.isBlank() ? requested : properties.getStorage().getFile();
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Output file is required");
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            throw new InvalidRequestException("Invalid file path: " + value);
        }
    }
}
