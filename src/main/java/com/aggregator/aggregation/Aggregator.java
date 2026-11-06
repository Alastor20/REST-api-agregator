package com.aggregator.aggregation;

import com.aggregator.api.ApiException;
import com.aggregator.api.ApiSource;
import com.aggregator.api.ApiSourceRegistry;
import com.aggregator.model.DataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Aggregator {
    private static final Logger log = LoggerFactory.getLogger(Aggregator.class);

    private final ApiSourceRegistry registry;

    public Aggregator(ApiSourceRegistry registry) {
        this.registry = registry;
    }

    public List<DataItem> aggregate(List<String> sourceNames, Map<String, Map<String, String>> overrides) throws ApiException {
        List<DataItem> result = new ArrayList<>();
        for (String name : sourceNames) {
            ApiSource source = registry.require(name);
            Map<String, String> params = overrides.getOrDefault(name, source.defaultParams());
            log.debug("Fetching source={} params={}", name, params);
            result.addAll(source.fetch(params));
        }
        log.info("Aggregation complete: {} item(s) total", result.size());
        return result;
    }
}