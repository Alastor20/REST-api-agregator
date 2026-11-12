package com.aggregator.config;

import com.aggregator.aggregation.Aggregator;
import com.aggregator.api.ApiSourceRegistry;
import com.aggregator.api.CurrencyApiSource;
import com.aggregator.api.OpenLibraryApiSource;
import com.aggregator.api.WeatherApiSource;
import com.aggregator.http.HttpClient;
import com.aggregator.mapper.CurrencyDataMapper;
import com.aggregator.mapper.OpenLibraryDataMapper;
import com.aggregator.mapper.WeatherDataMapper;
import com.aggregator.storage.DataStorageFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class CoreConfig {

    @Bean("apiObjectMapper")
    public ObjectMapper apiObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public HttpClient httpClient() {
        return new HttpClient();
    }

    @Bean
    public WeatherApiSource weatherApiSource(HttpClient httpClient, @Qualifier("apiObjectMapper") ObjectMapper apiObjectMapper) {
        return new WeatherApiSource(httpClient, new WeatherDataMapper(), apiObjectMapper);
    }

    @Bean
    public CurrencyApiSource currencyApiSource(HttpClient httpClient, @Qualifier("apiObjectMapper") ObjectMapper apiObjectMapper) {
        return new CurrencyApiSource(httpClient, new CurrencyDataMapper(), apiObjectMapper);
    }

    @Bean
    public OpenLibraryApiSource openLibraryApiSource(HttpClient httpClient, @Qualifier("apiObjectMapper") ObjectMapper apiObjectMapper) {
        return new OpenLibraryApiSource(httpClient, new OpenLibraryDataMapper(), apiObjectMapper);
    }

    @Bean
    public ApiSourceRegistry apiSourceRegistry(WeatherApiSource weatherApiSource, CurrencyApiSource currencyApiSource, OpenLibraryApiSource openLibraryApiSource) {
        ApiSourceRegistry registry = new ApiSourceRegistry();
        registry.register(weatherApiSource);
        registry.register(currencyApiSource);
        registry.register(openLibraryApiSource);
        return registry;
    }

    @Bean
    public Aggregator aggregator(ApiSourceRegistry apiSourceRegistry) {
        return new Aggregator(apiSourceRegistry);
    }

    @Bean
    public DataStorageFactory dataStorageFactory(@Qualifier("apiObjectMapper") ObjectMapper apiObjectMapper) {
        return new DataStorageFactory(apiObjectMapper);
    }
}
