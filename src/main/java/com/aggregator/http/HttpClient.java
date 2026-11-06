package com.aggregator.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpClient {
    private final java.net.http.HttpClient client;

    public HttpClient() {
        this.client = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String get(String url) throws HttpException {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new HttpException("HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new HttpException("Request failed for " + url, e);
        }
    }
}