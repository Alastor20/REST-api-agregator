package com.aggregator.web.dto;

import java.util.List;
import java.util.Map;

public record PollingStatusResponse(boolean running, Map<String, List<Map<String, String>>> activeSources, int n,
                                    long t, String startedAt, String format, String file, String mode) {
}
