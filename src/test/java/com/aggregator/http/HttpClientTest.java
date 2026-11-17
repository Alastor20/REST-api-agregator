package com.aggregator.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpClientTest {

    @Mock
    private java.net.http.HttpClient delegate;
    @Mock
    private HttpResponse<String> response;

    private HttpClient client;

    @BeforeEach
    void setUp() {
        client = new HttpClient(delegate);
    }

    @Test
    void returnsBodyForSuccessfulResponse() throws Exception {
        when(delegate.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"ok\":true}");

        assertThat(client.get("https://example.com/data?q=1")).isEqualTo("{\"ok\":true}");
    }

    @Test
    void throwsForErrorStatus() throws Exception {
        when(delegate.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);
        when(response.statusCode()).thenReturn(503);

        assertThatThrownBy(() -> client.get("https://example.com/fail"))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP 503")
                .hasMessageContaining("https://example.com/fail");
    }

    @Test
    void wrapsIoFailure() throws Exception {
        when(delegate.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException("connection reset"));

        assertThatThrownBy(() -> client.get("https://example.com/io"))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Request failed")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsInterruption() throws Exception {
        when(delegate.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new InterruptedException("interrupted"));

        assertThatThrownBy(() -> client.get("https://example.com/interrupted"))
                .isInstanceOf(HttpException.class)
                .hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    void rejectsMalformedUrlBeforeCallingDelegate() {
        assertThatThrownBy(() -> client.get("://bad"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(delegate);
    }
}
