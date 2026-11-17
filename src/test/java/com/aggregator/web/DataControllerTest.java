package com.aggregator.web;

import com.aggregator.model.DataItem;
import com.aggregator.polling.PollingService;
import com.aggregator.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataController.class)
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PollingService pollingService;

    @Test
    void returnsAllItems() throws Exception {
        when(pollingService.loadData(null)).thenReturn(List.of(item("weather"), item("currency")));

        mockMvc.perform(get("/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("weather"))
                .andExpect(jsonPath("$[0].timestamp").value("2026-01-02T03:04:05Z"))
                .andExpect(jsonPath("$[0].queryParams.city").value("Berlin"))
                .andExpect(jsonPath("$[0].data.temperature").value(5))
                .andExpect(jsonPath("$[1].source").value("currency"));

        verify(pollingService).loadData(null);
    }

    @Test
    void filtersBySource() throws Exception {
        when(pollingService.loadData("weather")).thenReturn(List.of(item("weather")));

        mockMvc.perform(get("/data").param("source", "weather"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("weather"));

        verify(pollingService).loadData("weather");
    }

    @Test
    void returnsEmptyArrayWhenThereIsNoData() throws Exception {
        when(pollingService.loadData(null)).thenReturn(List.of());

        mockMvc.perform(get("/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void storageFailureReturnsServerError() throws Exception {
        when(pollingService.loadData(null)).thenThrow(new StorageException("cannot read"));

        mockMvc.perform(get("/data"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("cannot read"))
                .andExpect(jsonPath("$.path").value("/data"));
    }

    private DataItem item(String source) {
        return new DataItem(source, Instant.parse("2026-01-02T03:04:05Z"),
                Map.of("city", "Berlin"), Map.of("temperature", 5));
    }
}
