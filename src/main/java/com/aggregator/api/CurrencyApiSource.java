package com.aggregator.api;

import com.aggregator.http.HttpClient;
import com.aggregator.mapper.DataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class CurrencyApiSource extends ApiSource {
    public CurrencyApiSource(HttpClient httpClient, DataMapper dataMapper, ObjectMapper apiMapper) {
        super(httpClient, dataMapper, apiMapper, "https://api.frankfurter.dev/v1/latest");
    }

    @Override
    public String getName() {
        return "currency";
    }

    @Override
    public Map<String, String> defaultParams() {
        Map<String, String> m = new HashMap<>();
        m.put("from", "USD");
        m.put("to", "EUR");
        return m;
    }
}