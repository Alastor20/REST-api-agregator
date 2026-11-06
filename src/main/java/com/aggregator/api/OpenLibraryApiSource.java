package com.aggregator.api;

import com.aggregator.http.HttpClient;
import com.aggregator.mapper.DataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class OpenLibraryApiSource extends ApiSource {
    public OpenLibraryApiSource(HttpClient httpClient, DataMapper dataMapper, ObjectMapper apiMapper) {
        super(httpClient, dataMapper, apiMapper, "https://openlibrary.org/search.json");
    }

    @Override
    public String getName() {
        return "books";
    }

    @Override
    public Map<String, String> defaultParams() {
        Map<String, String> m = new HashMap<>();
        m.put("q", "harry potter");
        m.put("limit", "5");
        return m;
    }
}