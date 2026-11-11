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
        String formattedParams = formatParams(params);
        log.info("[START] {} {}", getName(), formattedParams);
        try {
            String body = httpClient.get(url);
            Object parsed = apiMapper.readValue(body, Object.class);
            List<DataItem> items = dataMapper.map(parsed, getName(), params);
            log.info("[END] {} {} OK items={}", getName(), formattedParams, items.size());
            return items;
        } catch (HttpException | JsonProcessingException e) {
            log.warn("[END] {} {} ERROR: {}", getName(), formattedParams, e.getMessage());
            throw new ApiException("Failed to fetch " + getName(), e);
        } catch (RuntimeException e) {
            log.error("[END] {} {} ERROR: {}", getName(), formattedParams, e.getMessage());
            throw new ApiException("Failed to process " + getName(), e);
        }
    }

    private String formatParams(Map<String, String> params) {
        if (params.isEmpty()) {
            return "(no params)";
        }
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" "));
    }
}