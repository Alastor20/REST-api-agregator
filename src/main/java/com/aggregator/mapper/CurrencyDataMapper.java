package com.aggregator.mapper;

import com.aggregator.model.DataItem;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CurrencyDataMapper implements DataMapper {
    @Override
    public List<DataItem> map(Object parsedData, String source, Map<String, String> queryParams) {
        Map<String, Object> root = (Map<String, Object>) parsedData;
        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : List.of("amount", "base", "date", "rates")) {
            if (root.containsKey(key)) data.put(key, root.get(key));
        }
        return List.of(new DataItem(source, Instant.now(), queryParams, data));
    }
}