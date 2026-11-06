package com.aggregator.cli;

import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class CommandLineParser {
    private static void parseQuery(String spec, CommandArgs ca) {
        int dot = spec.indexOf('.');
        int eq = spec.indexOf('=');
        if (dot < 0 || eq < 0 || dot > eq) {
            throw new IllegalArgumentException("Bad --query: " + spec);
        }
        String source = spec.substring(0, dot);
        String key = spec.substring(dot + 1, eq);
        String value = spec.substring(eq + 1);
        ca.queryOverrides.computeIfAbsent(source, k -> new LinkedHashMap<>()).put(key, value);
    }

    private static void require(boolean condition, String message) {
        if (condition) throw new IllegalArgumentException(message);
    }

    public CommandArgs parse(String[] args) {
        if (args.length == 0) throw new IllegalArgumentException("Command required: poll|show");

        CommandArgs ca = new CommandArgs();
        ca.command = args[0];
        if (!ca.command.equals("poll") && !ca.command.equals("show")) {
            throw new IllegalArgumentException("Unknown command: " + ca.command);
        }

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--apis" -> ca.apis = Arrays.asList(args[++i].split(","));
                case "--format" -> ca.format = OutputFormat.fromString(args[++i]);
                case "--mode" -> ca.mode = WriteMode.valueOf(args[++i].toUpperCase());
                case "--file" -> ca.file = Path.of(args[++i]);
                case "--source" -> ca.sourceFilter = args[++i];
                case "--query" -> parseQuery(args[++i], ca);
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }

        if (ca.command.equals("poll")) {
            require(ca.apis.isEmpty(), "--apis required for poll");
            require(ca.format == null, "--format required for poll");
            require(ca.mode == null, "--mode required for poll");
            require(ca.file == null, "--file required for poll");
        } else {
            require(ca.file == null, "--file required for show");
        }
        return ca;
    }
}