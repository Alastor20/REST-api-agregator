package com.aggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Polling polling = new Polling();
    private Storage storage = new Storage();
    private Map<String, Source> sources = new LinkedHashMap<>();

    public Polling getPolling() {
        return polling;
    }

    public void setPolling(Polling polling) {
        this.polling = polling;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Map<String, Source> getSources() {
        return sources;
    }

    public void setSources(Map<String, Source> sources) {
        this.sources = sources;
    }

    public static class Polling {
        private int n = 3;
        private long t = 10;

        public int getN() {
            return n;
        }

        public void setN(int n) {
            this.n = n;
        }

        public long getT() {
            return t;
        }

        public void setT(long t) {
            this.t = t;
        }
    }

    public static class Storage {
        private String format = "json";
        private String file = "data/results.json";
        private String mode = "create";

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Source {
        private List<Map<String, String>> params = new ArrayList<>();

        public List<Map<String, String>> getParams() {
            return params;
        }

        public void setParams(List<Map<String, String>> params) {
            this.params = params;
        }
    }
}
