package com.aggregator.model;

import java.time.Instant;
import java.util.Map;

public record DataItem(String source, Instant timestamp, Map<String, String> queryParams, Map<String, Object> data) {
    @Override
    public String toString() {
        return "[" + source + "] " + timestamp + " query=" + queryParams + " data=" + data;
    }
}