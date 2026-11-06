package com.aggregator.cli;

import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandArgs {
    public String command;
    public List<String> apis = new ArrayList<>();
    public OutputFormat format;
    public WriteMode mode;
    public Path file;
    public String sourceFilter;
    public Map<String, Map<String, String>> queryOverrides = new HashMap<>();
}