package com.aggregator.mapper;

import com.aggregator.model.DataItem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyDataMapperTest {

    private final CurrencyDataMapper mapper = new CurrencyDataMapper();

    @Test
    void mapsKnownFieldsIncludingNestedRates() {
        Map<String, Object> rates = Map.of("EUR", 0.91, "GBP", 0.79);
        Map<String, Object> root = new HashMap<>();
        root.put("amount", 1.0);
        root.put("base", "USD");
        root.put("date", "2026-01-02");
        root.put("rates", rates);
        root.put("ignored", true);

        List<DataItem> items = mapper.map(root, "currency", Map.of("from", "USD"));

        assertThat(items).hasSize(1);
        DataItem item = items.getFirst();
        assertThat(item.source()).isEqualTo("currency");
        assertThat(item.queryParams()).containsEntry("from", "USD");
        assertThat(item.data())
                .containsEntry("amount", 1.0)
                .containsEntry("base", "USD")
                .containsEntry("date", "2026-01-02")
                .containsEntry("rates", rates)
                .doesNotContainKey("ignored");
    }

    @Test
    void skipsMissingFields() {
        List<DataItem> items = mapper.map(new HashMap<>(), "currency", Map.of());

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().data()).isEmpty();
    }
}
