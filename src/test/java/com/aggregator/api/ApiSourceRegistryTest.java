package com.aggregator.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiSourceRegistryTest {

    @Test
    void registersAndResolvesSources() {
        ApiSource weather = mock(ApiSource.class);
        when(weather.getName()).thenReturn("weather");

        ApiSourceRegistry registry = new ApiSourceRegistry();
        registry.register(weather);

        assertThat(registry.get("weather")).isSameAs(weather);
        assertThat(registry.require("weather")).isSameAs(weather);
        assertThat(registry.names()).containsExactly("weather");
        assertThat(registry.get("missing")).isNull();
    }

    @Test
    void requireFailsForUnknownSource() {
        ApiSourceRegistry registry = new ApiSourceRegistry();

        assertThatThrownBy(() -> registry.require("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }
}
