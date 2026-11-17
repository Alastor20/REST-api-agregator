package com.aggregator.api;

import com.aggregator.http.HttpClient;
import com.aggregator.mapper.DataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApiSourcesTest {

    private final HttpClient httpClient = mock(HttpClient.class);
    private final DataMapper dataMapper = mock(DataMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void weatherSourceExposesNameAndDefaultParams() {
        WeatherApiSource source = new WeatherApiSource(httpClient, dataMapper, objectMapper);

        assertThat(source.getName()).isEqualTo("weather");
        assertThat(source.defaultParams())
                .containsEntry("latitude", "52.52")
                .containsEntry("longitude", "13.41")
                .containsEntry("current_weather", "true");
    }

    @Test
    void currencySourceExposesNameAndDefaultParams() {
        CurrencyApiSource source = new CurrencyApiSource(httpClient, dataMapper, objectMapper);

        assertThat(source.getName()).isEqualTo("currency");
        assertThat(source.defaultParams())
                .isEqualTo(Map.of("from", "USD", "to", "EUR"));
    }

    @Test
    void booksSourceExposesNameAndDefaultParams() {
        OpenLibraryApiSource source = new OpenLibraryApiSource(httpClient, dataMapper, objectMapper);

        assertThat(source.getName()).isEqualTo("books");
        assertThat(source.defaultParams())
                .containsEntry("q", "harry potter")
                .containsEntry("limit", "5");
    }
}
