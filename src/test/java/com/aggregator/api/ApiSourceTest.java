package com.aggregator.api;

import com.aggregator.http.HttpClient;
import com.aggregator.http.HttpException;
import com.aggregator.mapper.DataMapper;
import com.aggregator.model.DataItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiSourceTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final DataMapper dataMapper = mock(DataMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiSource source = new TestApiSource(httpClient, dataMapper, objectMapper);

    @Test
    void buildsUrlWithEncodedQueryParameters() throws ApiException, HttpException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("city", "New York");
        params.put("q", "a&b");

        when(httpClient.get("https://example.test/api?city=New+York&q=a%26b"))
                .thenReturn("{\"ok\":true}");
        when(dataMapper.map(anyMap(), eq("test"), eq(params))).thenReturn(List.of());

        source.fetch(params);

        verify(httpClient).get("https://example.test/api?city=New+York&q=a%26b");
    }

    @Test
    void usesBaseUrlWithoutParameters() throws ApiException, HttpException {
        when(httpClient.get("https://example.test/api")).thenReturn("{}");
        when(dataMapper.map(any(), eq("test"), eq(Map.of()))).thenReturn(List.of());

        source.fetch(Map.of());

        verify(httpClient).get("https://example.test/api");
    }

    @Test
    void returnsMappedItemsOnSuccess() throws ApiException, HttpException {
        Map<String, String> params = Map.of("city", "Berlin");
        DataItem item = new DataItem("test", Instant.EPOCH, params, Map.of("value", 1));
        when(httpClient.get(anyString())).thenReturn("{\"value\":1}");
        when(dataMapper.map(any(), eq("test"), eq(params))).thenReturn(List.of(item));

        List<DataItem> result = source.fetch(params);

        assertThat(result).containsExactly(item);
    }

    @Test
    void wrapsHttpFailures() throws HttpException {
        when(httpClient.get(anyString())).thenThrow(new HttpException("timeout"));

        assertThatThrownBy(() -> source.fetch(Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Failed to fetch test")
                .hasCauseInstanceOf(HttpException.class);
    }

    @Test
    void wrapsJsonFailures() throws HttpException {
        when(httpClient.get(anyString())).thenReturn("not json");

        assertThatThrownBy(() -> source.fetch(Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Failed to fetch test");
    }

    @Test
    void wrapsRuntimeFailuresFromMapper() throws HttpException {
        when(httpClient.get(anyString())).thenReturn("{}");
        when(dataMapper.map(any(), anyString(), anyMap())).thenThrow(new IllegalStateException("bad mapping"));

        assertThatThrownBy(() -> source.fetch(Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Failed to process test")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    private static final class TestApiSource extends ApiSource {

        private TestApiSource(HttpClient httpClient, DataMapper dataMapper, ObjectMapper apiMapper) {
            super(httpClient, dataMapper, apiMapper, "https://example.test/api");
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public Map<String, String> defaultParams() {
            return Map.of();
        }
    }
}
