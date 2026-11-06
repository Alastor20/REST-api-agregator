package com.aggregator.api;

import com.aggregator.http.HttpClient;
import com.aggregator.http.HttpException;
import com.aggregator.mapper.DataMapper;
import com.aggregator.model.DataItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ApiSource {
    protected final HttpClient httpClient;
    protected final DataMapper dataMapper;
    protected final ObjectMapper apiMapper;
    protected final String baseUrl;

    protected ApiSource(HttpClient httpClient, DataMapper dataMapper, ObjectMapper apiMapper, String baseUrl) {
        this.httpClient = httpClient;
        this.dataMapper = dataMapper;
        this.apiMapper = apiMapper;
        this.baseUrl = baseUrl;
    }

    protected String buildUrl(Map<String, String> params) {
        if (params.isEmpty()) return baseUrl;
        String q = params.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return baseUrl + "?" + q;
    }

    public abstract String getName();

    public abstract Map<String, String> defaultParams();

    public List<DataItem> fetch(Map<String, String> params) throws ApiException {
        Logger log = LoggerFactory.getLogger(getClass());
        String url = buildUrl(params);
        log.info("[START] {} params={}", getName(), params);
        try {
            String body = httpClient.get(url);
            Object parsed = apiMapper.readValue(body, Object.class);
            List<DataItem> items = dataMapper.map(parsed, getName(), params);
            log.info("[END] {} params={} OK items={}", getName(), params, items.size());
            return items;
        } catch (HttpException | JsonProcessingException e) {
            log.warn("[END] {} params={} FAIL: {}", getName(), params, e.getMessage());
            throw new ApiException("Failed to fetch " + getName(), e);
        }
    }
}