package com.aggregator.web;

import com.aggregator.web.dto.PollingActionResponse;
import com.aggregator.web.dto.PollingRequestResponse;
import com.aggregator.web.dto.PollingStatusResponse;
import com.aggregator.polling.PollingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/polling")
public class PollingController {

    private final PollingService pollingService;

    public PollingController(PollingService pollingService) {
        this.pollingService = pollingService;
    }

    @PostMapping("/start")
    public PollingActionResponse start(@RequestBody(required = false) PollingRequestResponse request) {
        pollingService.start(request);
        return new PollingActionResponse("started", "Polling started");
    }

    @PostMapping("/stop")
    public PollingActionResponse stop() {
        pollingService.stop();
        return new PollingActionResponse("stopped", "Polling stopped");
    }

    @GetMapping("/status")
    public PollingStatusResponse status() {
        return toResponse(pollingService.status());
    }


    private PollingStatusResponse toResponse(PollingStatusResponse status) {
        return new PollingStatusResponse(status.running(), status.activeSources(), status.n(), status.t(), status.startedAt() == null ? null : status.startedAt().toString(), status.format(), status.file(), status.mode());
    }
}
