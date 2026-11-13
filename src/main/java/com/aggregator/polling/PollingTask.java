package com.aggregator.polling;

import com.aggregator.api.ApiException;
import com.aggregator.api.ApiSource;
import com.aggregator.model.DataItem;
import com.aggregator.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class PollingTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PollingTask.class);

    private final ApiSource source;
    private final Map<String, String> params;
    private final PollingSession session;

    public PollingTask(ApiSource source, Map<String, String> params, PollingSession session) {
        this.source = source;
        this.params = params;
        this.session = session;
    }

    @Override
    public void run() {
        session.execute(() -> {
            try {
                List<DataItem> items = source.fetch(params);
                session.save(items);
                log.debug("Polled source={} params={} items={}", source.getName(), params, items.size());
            } catch (ApiException e) {
                log.warn("Poll failed source={} params={}: {}", source.getName(), params, e.getMessage());
            } catch (StorageException e) {
                log.error("Storage failed source={} params={}", source.getName(), params, e);
            } catch (RuntimeException e) {
                log.error("Unexpected polling failure source={} params={}", source.getName(), params, e);
            }
        });
    }
}
