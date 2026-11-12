package com.aggregator.web.dto;

import com.aggregator.model.DataItem;

import java.util.Map;

public record DataItemResponse(String source, String timestamp, Map<String, String> queryParams,
                               Map<String, Object> data) {
    public static DataItemResponse from(DataItem item) {
        return new DataItemResponse(item.source(), item.timestamp().toString(), item.queryParams(), item.data());
    }
}
