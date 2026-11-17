package com.aggregator.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    @Test
    void exposesDefaults() {
        AppProperties properties = new AppProperties();

        assertThat(properties.getPolling().getN()).isEqualTo(3);
        assertThat(properties.getPolling().getT()).isEqualTo(10L);
        assertThat(properties.getStorage().getFormat()).isEqualTo("json");
        assertThat(properties.getStorage().getFile()).isEqualTo("data/results.json");
        assertThat(properties.getStorage().getMode()).isEqualTo("create");
        assertThat(properties.getSources()).isEmpty();
    }

    @Test
    void supportsSettersAndNestedSources() {
        AppProperties properties = new AppProperties();
        AppProperties.Polling polling = new AppProperties.Polling();
        polling.setN(4);
        polling.setT(30);
        AppProperties.Storage storage = new AppProperties.Storage();
        storage.setFormat("csv");
        storage.setFile("out.csv");
        storage.setMode("append");
        AppProperties.Source source = new AppProperties.Source();
        source.setParams(List.of(Map.of("q", "test")));

        properties.setPolling(polling);
        properties.setStorage(storage);
        properties.setSources(Map.of("books", source));

        assertThat(properties.getPolling()).isSameAs(polling);
        assertThat(properties.getStorage()).isSameAs(storage);
        assertThat(properties.getSources()).containsEntry("books", source);
        assertThat(properties.getSources().get("books").getParams()).containsExactly(Map.of("q", "test"));
    }
}
