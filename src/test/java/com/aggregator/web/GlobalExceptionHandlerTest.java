package com.aggregator.web;

import com.aggregator.polling.InvalidRequestException;
import com.aggregator.polling.PollingConflictException;
import com.aggregator.storage.StorageException;
import com.aggregator.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/data");

    @Test
    void mapsInvalidRequestToBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidRequest(new InvalidRequestException("bad input"), request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "bad input");
    }

    @Test
    void mapsConflictToConflictStatus() {
        ResponseEntity<ErrorResponse> response =
                handler.handleConflict(new PollingConflictException("already running"), request);

        assertResponse(response, HttpStatus.CONFLICT, "already running");
    }

    @Test
    void mapsGenericBadRequests() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new IllegalArgumentException("invalid"), request);

        assertResponse(response, HttpStatus.BAD_REQUEST, "invalid");
    }

    @Test
    void mapsStorageFailuresToServerError() {
        ResponseEntity<ErrorResponse> response =
                handler.handleStorage(new StorageException("disk error"), request);

        assertResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "disk error");
    }

    private void assertResponse(ResponseEntity<ErrorResponse> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(status.value());
        assertThat(body.error()).isEqualTo(status.getReasonPhrase());
        assertThat(body.message()).isEqualTo(message);
        assertThat(body.path()).isEqualTo("/data");
        assertThat(body.timestamp()).isNotBlank();
    }
}
