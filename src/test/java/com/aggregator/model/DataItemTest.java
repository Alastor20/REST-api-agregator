package com.aggregator.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataItemTest {

    @Test
    void buildsReadableToString() {
        DataItem item = new DataItem("weather", Instant.EPOCH, Map.of("city", "Berlin"), Map.of("temperature", 5));

        assertThat(item.toString())
                .contains("[weather]")
                .contains("1970-01-01T00:00:00Z")
                .contains("city=Berlin")
                .contains("temperature=5");
    }
}
