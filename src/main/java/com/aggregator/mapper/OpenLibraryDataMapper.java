package com.aggregator.mapper;

import com.aggregator.model.DataItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenLibraryDataMapper implements DataMapper {

    @Override
    public List<DataItem> map(Object parsedData, String source, Map<String, String> queryParams) {
        if (!(parsedData instanceof Map<?, ?> root)) return List.of();

        List<DataItem> result = new ArrayList<>();
        Object docsObj = root.get("docs");
        if (!(docsObj instanceof List<?> docs)) return List.of();

        for (Object docObj : docs) {
            if (!(docObj instanceof Map<?, ?>)) continue;
            Map<String, Object> doc = (Map<String, Object>) docObj;

            Map<String, Object> data = new LinkedHashMap<>();
            for (String key : List.of("key", "title", "author_name", "first_publish_year", "language", "subject", "publisher", "edition_count", "isbn", "number_of_pages_median")) {
                if (doc.containsKey(key)) data.put(key, doc.get(key));
            }
            result.add(new DataItem(source, Instant.now(), queryParams, data));
        }
        return result;
    }
}