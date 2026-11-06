package com.aggregator.model;

public enum OutputFormat {
    CSV, JSON;

    public static OutputFormat fromString(String s) {
        return valueOf(s.trim().toUpperCase());
    }
}