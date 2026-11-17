package com.aggregator.mapper;

import com.aggregator.model.DataItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenLibraryDataMapperTest {

    private final OpenLibraryDataMapper mapper = new OpenLibraryDataMapper();

    @Test
    void returnsEmptyForNonMapResponse() {
        assertThat(mapper.map(List.of("not-a-map"), "books", Map.of())).isEmpty();
    }

    @Test
    void returnsEmptyWhenDocsIsMissingOrNotAList() {
        assertThat(mapper.map(Map.of("numFound", 1), "books", Map.of())).isEmpty();
        assertThat(mapper.map(Map.of("docs", "oops"), "books", Map.of())).isEmpty();
    }

    @Test
    void mapsEveryValidDocAndSkipsInvalidEntries() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("key", "/works/1");
        doc.put("title", "Test Book");
        doc.put("author_name", List.of("Author"));
        doc.put("first_publish_year", 2000);
        doc.put("edition_count", 3);
        doc.put("ignored", "value");

        List<Object> docs = new ArrayList<>();
        docs.add("not-a-map");
        docs.add(doc);

        Map<String, Object> root = Map.of("docs", docs, "extra", 1);
        List<DataItem> items = mapper.map(root, "books", Map.of("q", "test"));

        assertThat(items).hasSize(1);
        DataItem item = items.get(0);
        assertThat(item.source()).isEqualTo("books");
        assertThat(item.queryParams()).containsEntry("q", "test");
        assertThat(item.data())
                .containsEntry("key", "/works/1")
                .containsEntry("title", "Test Book")
                .containsEntry("author_name", List.of("Author"))
                .containsEntry("first_publish_year", 2000)
                .containsEntry("edition_count", 3)
                .doesNotContainKey("ignored");
    }
}
