package com.aggregator.web;

import com.aggregator.polling.PollingService;
import com.aggregator.storage.StorageException;
import com.aggregator.web.dto.DataItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DataController {

    private final PollingService pollingService;

    public DataController(PollingService pollingService) {
        this.pollingService = pollingService;
    }

    @GetMapping("/data")
    public List<DataItemResponse> data(@RequestParam(name = "source", required = false) String source) throws StorageException {
        return pollingService.loadData(source).stream().map(DataItemResponse::from).toList();
    }
}
