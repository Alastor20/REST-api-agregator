package com.aggregator.mapper;

import com.aggregator.model.DataItem;

import java.util.List;
import java.util.Map;

public interface DataMapper {
    List<DataItem> map(Object parsedData, String source, Map<String, String> queryParams);
}