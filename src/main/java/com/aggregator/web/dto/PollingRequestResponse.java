package com.aggregator.web.dto;

import java.util.List;
import java.util.Map;

public record PollingRequestResponse(List<SourceRequest> sources, String format, String mode, String file) {
    public record SourceRequest(String name, List<Map<String, String>> params) {
    }
}
