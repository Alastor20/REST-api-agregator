package com.aggregator.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiSourceRegistry {
    private final Map<String, ApiSource> sources = new HashMap<>();

    public void register(ApiSource source) {
        sources.put(source.getName(), source);
    }

    public ApiSource get(String name) {
        return sources.get(name);
    }

    public ApiSource require(String name) {
        ApiSource s = sources.get(name);
        if (s == null) throw new IllegalArgumentException("Unknown API source: " + name);
        return s;
    }

    public List<String> names() {
        return new ArrayList<>(sources.keySet());
    }
}