package com.aggregator;

import com.aggregator.aggregation.Aggregator;
import com.aggregator.api.ApiSourceRegistry;
import com.aggregator.api.CurrencyApiSource;
import com.aggregator.api.OpenLibraryApiSource;
import com.aggregator.api.WeatherApiSource;
import com.aggregator.cli.CliCommands;
import com.aggregator.cli.CommandArgs;
import com.aggregator.cli.CommandLineParser;
import com.aggregator.http.HttpClient;
import com.aggregator.mapper.CurrencyDataMapper;
import com.aggregator.mapper.OpenLibraryDataMapper;
import com.aggregator.mapper.WeatherDataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static ApiSourceRegistry buildRegistry(ObjectMapper objectMapper) {
        HttpClient http = new HttpClient();
        ApiSourceRegistry registry = new ApiSourceRegistry();
        registry.register(new WeatherApiSource(http, new WeatherDataMapper(), objectMapper));
        registry.register(new CurrencyApiSource(http, new CurrencyDataMapper(), objectMapper));
        registry.register(new OpenLibraryApiSource(http, new OpenLibraryDataMapper(), objectMapper));
        return registry;
    }

    void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            CommandArgs cmdArgs = new CommandLineParser().parse(args);
            ApiSourceRegistry registry = buildRegistry(objectMapper);
            CliCommands cli = new CliCommands(new Aggregator(registry), objectMapper);
            cli.execute(cmdArgs);
        } catch (IllegalArgumentException e) {
            log.error("Argument error: {}", e.getMessage());
            log.info("""
                    Usage:
                      poll --apis <name1,name2,...> --format <csv|json> --mode <create|append> --file <path>
                           [--query <source>.<key>=<value> ...]
                      show --file <path> [--format <csv|json>] [--source <name>]
                    """);
        } catch (Exception e) {
            log.error("<{}> {}", e.getClass().getName(), e.getMessage());
            System.exit(1);
        }
    }
}