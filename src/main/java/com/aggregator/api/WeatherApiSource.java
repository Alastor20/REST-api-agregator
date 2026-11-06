package com.aggregator.api;

import com.aggregator.http.HttpClient;
import com.aggregator.mapper.DataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class WeatherApiSource extends ApiSource {
    public WeatherApiSource(HttpClient httpClient, DataMapper dataMapper, ObjectMapper apiMapper) {
        super(httpClient, dataMapper, apiMapper, "https://api.open-meteo.com/v1/forecast");
    }

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public Map<String, String> defaultParams() {
        Map<String, String> m = new HashMap<>();
        m.put("latitude", "52.52");
        m.put("longitude", "13.41");
        m.put("current_weather", "true");
        return m;
    }
}