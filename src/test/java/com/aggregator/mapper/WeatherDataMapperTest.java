package com.aggregator.mapper;

import com.aggregator.model.DataItem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherDataMapperTest {

    private final WeatherDataMapper mapper = new WeatherDataMapper();

    @Test
    void mapsKnownFieldsIncludingNestedObject() {
        Map<String, Object> currentWeather = Map.of("temperature", 7.5, "windspeed", 3.1);
        Map<String, Object> root = new HashMap<>();
        root.put("latitude", 52.52);
        root.put("longitude", 13.41);
        root.put("timezone", "GMT");
        root.put("elevation", 38.0);
        root.put("utc_offset_seconds", 0);
        root.put("generationtime_ms", 0.2);
        root.put("current_weather", currentWeather);
        root.put("ignored", "value");

        Map<String, String> query = Map.of("latitude", "52.52");
        List<DataItem> items = mapper.map(root, "weather", query);

        assertThat(items).hasSize(1);
        DataItem item = items.get(0);
        assertThat(item.source()).isEqualTo("weather");
        assertThat(item.queryParams()).isEqualTo(query);
        assertThat(item.timestamp()).isNotNull();
        assertThat(item.data())
                .containsEntry("latitude", 52.52)
                .containsEntry("longitude", 13.41)
                .containsEntry("timezone", "GMT")
                .containsEntry("current_weather", currentWeather)
                .doesNotContainKey("ignored");
    }

    @Test
    void skipsMissingFields() {
        List<DataItem> items = mapper.map(new HashMap<>(), "weather", Map.of());

        assertThat(items).hasSize(1);
        assertThat(items.get(0).data()).isEmpty();
    }
}
