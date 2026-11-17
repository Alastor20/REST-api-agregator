package com.aggregator.config;

import com.aggregator.aggregation.Aggregator;
import com.aggregator.api.ApiSourceRegistry;
import com.aggregator.api.CurrencyApiSource;
import com.aggregator.api.OpenLibraryApiSource;
import com.aggregator.api.WeatherApiSource;
import com.aggregator.http.HttpClient;
import com.aggregator.storage.DataStorageFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CoreConfigTest {

    private final CoreConfig config = new CoreConfig();
    private final HttpClient httpClient = mock(HttpClient.class);

    @Test
    void createsApiObjectMapperWithIsoDates() throws JsonProcessingException {
        ObjectMapper mapper = config.apiObjectMapper();

        assertThat(mapper.writeValueAsString(Instant.EPOCH)).isEqualTo("\"1970-01-01T00:00:00Z\"");
    }

    @Test
    void createsHttpClient() {
        assertThat(config.httpClient()).isNotNull();
    }

    @Test
    void createsRegisteredApiSources() {
        ObjectMapper mapper = config.apiObjectMapper();
        WeatherApiSource weather = config.weatherApiSource(httpClient, mapper);
        CurrencyApiSource currency = config.currencyApiSource(httpClient, mapper);
        OpenLibraryApiSource books = config.openLibraryApiSource(httpClient, mapper);

        ApiSourceRegistry registry = config.apiSourceRegistry(weather, currency, books);

        assertThat(registry.names()).containsExactlyInAnyOrder("weather", "currency", "books");
        assertThat(registry.get("weather")).isSameAs(weather);
        assertThat(registry.get("currency")).isSameAs(currency);
        assertThat(registry.get("books")).isSameAs(books);
    }

    @Test
    void createsAggregatorAndStorageFactory() {
        ObjectMapper mapper = config.apiObjectMapper();
        ApiSourceRegistry registry = config.apiSourceRegistry(
                config.weatherApiSource(httpClient, mapper),
                config.currencyApiSource(httpClient, mapper),
                config.openLibraryApiSource(httpClient, mapper));

        Aggregator aggregator = config.aggregator(registry);
        DataStorageFactory factory = config.dataStorageFactory(mapper);

        assertThat(aggregator).isNotNull();
        assertThat(factory.create(java.nio.file.Path.of("build/test.json"),
                com.aggregator.model.OutputFormat.JSON)).isNotNull();
    }
}
