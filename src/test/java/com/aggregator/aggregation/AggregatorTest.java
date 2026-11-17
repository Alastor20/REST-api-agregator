package com.aggregator.aggregation;

import com.aggregator.api.ApiException;
import com.aggregator.api.ApiSource;
import com.aggregator.api.ApiSourceRegistry;
import com.aggregator.model.DataItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AggregatorTest {

    private final ApiSourceRegistry registry = new ApiSourceRegistry();

    @Test
    void aggregatesConfiguredSourcesUsingOverrides() throws ApiException {
        DataItem first = new DataItem("a", Instant.EPOCH, Map.of("x", "1"), Map.of());
        DataItem second = new DataItem("b", Instant.EPOCH, Map.of("y", "2"), Map.of());

        ApiSource sourceA = mock(ApiSource.class);
        when(sourceA.getName()).thenReturn("a");
        when(sourceA.fetch(Map.of("x", "1"))).thenReturn(List.of(first));

        ApiSource sourceB = mock(ApiSource.class);
        when(sourceB.getName()).thenReturn("b");
        when(sourceB.fetch(Map.of("y", "2"))).thenReturn(List.of(second));

        registry.register(sourceA);
        registry.register(sourceB);

        Aggregator aggregator = new Aggregator(registry);
        List<DataItem> result = aggregator.aggregate(List.of("a", "b"), Map.of("a", Map.of("x", "1"), "b", Map.of("y", "2")));

        assertThat(result).containsExactly(first, second);
        verify(sourceA).fetch(Map.of("x", "1"));
        verify(sourceB).fetch(Map.of("y", "2"));
    }

    @Test
    void usesDefaultParamsWhenNoOverrideIsProvided() throws ApiException {
        ApiSource source = mock(ApiSource.class);
        when(source.getName()).thenReturn("a");
        when(source.defaultParams()).thenReturn(Map.of("default", "yes"));
        when(source.fetch(Map.of("default", "yes"))).thenReturn(List.of());

        registry.register(source);

        new Aggregator(registry).aggregate(List.of("a"), Map.of());

        verify(source).fetch(Map.of("default", "yes"));
    }

    @Test
    void rejectsUnknownSource() {
        Aggregator aggregator = new Aggregator(registry);

        assertThatThrownBy(() -> aggregator.aggregate(List.of("missing"), Map.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
