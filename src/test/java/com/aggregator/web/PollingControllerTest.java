package com.aggregator.web;

import com.aggregator.polling.InvalidRequestException;
import com.aggregator.polling.PollingConflictException;
import com.aggregator.polling.PollingService;
import com.aggregator.web.dto.PollingRequestResponse;
import com.aggregator.web.dto.PollingStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PollingController.class)
class PollingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PollingService pollingService;

    @Test
    void startReturnsAcceptedAction() throws Exception {
        doNothing().when(pollingService).start(any());

        mockMvc.perform(post("/polling/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sources":[{"name":"weather","params":[{"city":"Berlin"}]}],
                                 "format":"json","mode":"create","file":"data/results.json"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.message").value("Polling started"));

        verify(pollingService).start(any(PollingRequestResponse.class));
    }

    @Test
    void startWithoutBodyUsesDefaults() throws Exception {
        doNothing().when(pollingService).start(null);

        mockMvc.perform(post("/polling/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"));

        verify(pollingService).start(null);
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        doThrow(new InvalidRequestException("bad format")).when(pollingService).start(any());

        mockMvc.perform(post("/polling/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("bad format"))
                .andExpect(jsonPath("$.path").value("/polling/start"));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/polling/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void conflictReturnsConflictStatus() throws Exception {
        doThrow(new PollingConflictException("already running")).when(pollingService).start(any());

        mockMvc.perform(post("/polling/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("already running"));
    }

    @Test
    void stopReturnsStoppedAction() throws Exception {
        doNothing().when(pollingService).stop();

        mockMvc.perform(post("/polling/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("stopped"))
                .andExpect(jsonPath("$.message").value("Polling stopped"));

        verify(pollingService).stop();
    }

    @Test
    void stopConflictReturnsConflictStatus() throws Exception {
        doThrow(new PollingConflictException("not running")).when(pollingService).stop();

        mockMvc.perform(post("/polling/stop"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("not running"));
    }

    @Test
    void statusReturnsSessionDetails() throws Exception {
        when(pollingService.status()).thenReturn(new PollingStatusResponse(
                true,
                Map.of("weather", List.of(Map.of("city", "Berlin"))),
                2, 5, "2026-01-02T03:04:05Z", "json", "data/results.json", "create"));

        mockMvc.perform(get("/polling/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.activeSources.weather[0].city").value("Berlin"))
                .andExpect(jsonPath("$.n").value(2))
                .andExpect(jsonPath("$.t").value(5))
                .andExpect(jsonPath("$.startedAt").value("2026-01-02T03:04:05Z"))
                .andExpect(jsonPath("$.format").value("json"))
                .andExpect(jsonPath("$.file").value("data/results.json"))
                .andExpect(jsonPath("$.mode").value("create"));
    }

    @Test
    void statusWithoutSessionReturnsDefaults() throws Exception {
        when(pollingService.status()).thenReturn(new PollingStatusResponse(
                false, Map.of(), 2, 5, null, "json", "data/results.json", "create"));

        mockMvc.perform(get("/polling/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.activeSources").isEmpty());
    }
}
