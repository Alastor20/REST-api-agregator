package com.aggregator.polling;

import com.aggregator.api.ApiSource;
import com.aggregator.api.ApiSourceRegistry;
import com.aggregator.config.AppProperties;
import com.aggregator.model.DataItem;
import com.aggregator.model.OutputFormat;
import com.aggregator.model.WriteMode;
import com.aggregator.storage.DataStorage;
import com.aggregator.storage.DataStorageFactory;
import com.aggregator.storage.StorageException;
import com.aggregator.web.dto.PollingRequestResponse;
import com.aggregator.web.dto.PollingStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PollingServiceTest {

    @Mock
    private ApiSourceRegistry registry;
    @Mock
    private PollingScheduler scheduler;
    @Mock
    private DataStorageFactory storageFactory;
    @Mock
    private DataStorage storage;
    @Mock
    private ScheduledFuture<?> future;
    @Mock
    private ApiSource weatherSource;
    @Mock
    private ApiSource currencySource;

    private AppProperties properties;
    private PollingService service;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getPolling().setN(2);
        properties.getPolling().setT(5);
        properties.getSources().put("weather", source(
                List.of(Map.of("city", "Berlin"), Map.of("city", "Paris"))));
        properties.getSources().put("currency", source(List.of(Map.of("from", "USD"))));

        when(registry.names()).thenReturn(List.of("weather", "currency"));
        when(registry.get("weather")).thenReturn(weatherSource);
        when(registry.get("currency")).thenReturn(currencySource);
        doReturn(future).when(scheduler).scheduleWithFixedDelay(any(Runnable.class), anyLong());
        when(storageFactory.create(any(Path.class), any(OutputFormat.class))).thenReturn(storage);

        service = new PollingService(registry, scheduler, storageFactory, properties);
    }

    @Test
    void startsWithConfiguredDefaultsAndSchedulesEveryParameterSet() {
        service.start(null);

        verify(scheduler, times(3)).scheduleWithFixedDelay(any(Runnable.class), eq(5L));
        verify(storageFactory).create(Path.of("data/results.json"), OutputFormat.JSON);

        PollingStatusResponse status = service.status();
        assertThat(status.running()).isTrue();
        assertThat(status.activeSources()).containsOnlyKeys("weather", "currency");
        assertThat(status.activeSources().get("weather"))
                .containsExactly(Map.of("city", "Berlin"), Map.of("city", "Paris"));
        assertThat(status.n()).isEqualTo(2);
        assertThat(status.t()).isEqualTo(5);
        assertThat(status.startedAt()).isNotBlank();
        assertThat(status.format()).isEqualTo("json");
        assertThat(status.file()).isEqualTo(Path.of("data/results.json").toString());
        assertThat(status.mode()).isEqualTo("create");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("startRequests")
    void appliesRequestOverridesAndFallsBackToConfiguration(String scenario,
                                                            PollingRequestResponse.SourceRequest sourceRequest,
                                                            String format, String mode, String file,
                                                            List<Map<String, String>> expectedParams,
                                                            String expectedFormat, String expectedMode,
                                                            String expectedFile, int scheduledTasks) {
        service.start(new PollingRequestResponse(List.of(sourceRequest), format, mode, file));

        PollingStatusResponse status = service.status();
        assertThat(status.activeSources().get(sourceRequest.name())).containsExactlyElementsOf(expectedParams);
        assertThat(status.format()).isEqualTo(expectedFormat);
        assertThat(status.mode()).isEqualTo(expectedMode);
        assertThat(status.file()).isEqualTo(expectedFile);
        verify(storageFactory).create(Path.of(expectedFile), OutputFormat.valueOf(expectedFormat.toUpperCase()));
        verify(scheduler, times(scheduledTasks)).scheduleWithFixedDelay(any(Runnable.class), eq(5L));
    }

    @Test
    void rejectsSecondStartWhileRunning() {
        service.start(null);

        assertThatThrownBy(() -> service.start(null))
                .isInstanceOf(PollingConflictException.class)
                .hasMessageContaining("already running");
    }

    @Test
    void rejectsInvalidPollingSettings() {
        properties.getPolling().setN(0);
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);

        properties.getPolling().setN(2);
        properties.getPolling().setT(0);
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsUnknownRequestedSource() {
        PollingRequestResponse request = new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest("unknown", List.of(Map.of("q", "x")))),
                null, null, null);

        assertThatThrownBy(() -> service.start(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unknown API source");
    }

    @Test
    void rejectsDuplicateRequestedSource() {
        PollingRequestResponse request = new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest("weather", List.of(Map.of("city", "A"))),
                        new PollingRequestResponse.SourceRequest("weather", List.of(Map.of("city", "B")))),
                null, null, null);

        assertThatThrownBy(() -> service.start(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Duplicate source");
    }

    @Test
    void rejectsMissingSourceNameAndRequests() {
        assertThatThrownBy(() -> service.start(new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest(" ", List.of(Map.of("q", "x")))), null, null, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Source name is required");

        assertThatThrownBy(() -> service.start(new PollingRequestResponse(
                Collections.singletonList(null), null, null, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Source name is required");
    }

    @Test
    void rejectsEmptyAndNullParameterSets() {
        assertThatThrownBy(() -> service.start(new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest("weather", List.of(Map.of()))), null, null, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Empty parameter set");

        assertThatThrownBy(() -> service.start(new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest("weather", Collections.singletonList(null))),
                null, null, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Empty parameter set");
    }

    @Test
    void rejectsSourceWithoutConfiguredParams() {
        properties.getSources().clear();
        PollingRequestResponse request = new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest("weather", null)), null, null, null);

        assertThatThrownBy(() -> service.start(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("No parameter sets");
    }

    @Test
    void ignoresNullAndEmptyConfiguredParameterSets() {
        properties.getSources().get("weather").setParams(List.of());
        assertThatThrownBy(() -> service.start(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("No parameter sets configured");

        properties.getSources().get("weather").setParams(Collections.singletonList(null));
        assertThatThrownBy(() -> service.start(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("No parameter sets configured");
    }

    @Test
    void rejectsInvalidRequestFormatAndMode() {
        assertThatThrownBy(() -> service.start(new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest("weather", List.of(Map.of("q", "x")))),
                "xml", null, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid format");

        assertThatThrownBy(() -> service.start(new PollingRequestResponse(
                List.of(new PollingRequestResponse.SourceRequest("weather", List.of(Map.of("q", "x")))),
                null, "replace", null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid mode");
    }

    @Test
    void rejectsInvalidConfiguredFormatModeAndFile() {
        properties.getStorage().setFormat("xml");
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);

        properties.getStorage().setFormat("json");
        properties.getStorage().setMode("replace");
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);

        properties.getStorage().setMode("create");
        properties.getStorage().setFile("\u0000");
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsBlankConfiguredFormatModeAndFile() {
        properties.getStorage().setFormat(" ");
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);

        properties.getStorage().setFormat("json");
        properties.getStorage().setMode(" ");
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);

        properties.getStorage().setMode("create");
        properties.getStorage().setFile(" ");
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void stopsSessionWhenSchedulingFails() {
        when(scheduler.scheduleWithFixedDelay(any(Runnable.class), anyLong()))
                .thenThrow(new IllegalStateException("executor rejected"));

        assertThatThrownBy(() -> service.start(null)).isInstanceOf(IllegalStateException.class);
        assertThat(service.status().running()).isFalse();
    }

    @Test
    void stopFailsWhenNothingIsRunning() {
        assertThatThrownBy(service::stop)
                .isInstanceOf(PollingConflictException.class)
                .hasMessageContaining("not running");
    }

    @Test
    void stopStopsRunningSession() {
        service.start(null);

        service.stop();

        PollingStatusResponse status = service.status();
        assertThat(status.running()).isFalse();
        assertThat(status.activeSources()).isEmpty();
        assertThat(status.startedAt()).isNull();
        verify(future, atLeastOnce()).cancel(false);
    }

    @Test
    void reportsDefaultsWithoutSession() {
        PollingStatusResponse status = service.status();

        assertThat(status.running()).isFalse();
        assertThat(status.activeSources()).isEmpty();
        assertThat(status.n()).isEqualTo(2);
        assertThat(status.t()).isEqualTo(5);
        assertThat(status.startedAt()).isNull();
        assertThat(status.format()).isEqualTo("json");
        assertThat(status.file()).isEqualTo("data/results.json");
        assertThat(status.mode()).isEqualTo("create");
    }

    @Test
    void loadsDataFromDefaultStorageAndFiltersBySource() throws StorageException {
        DataItem weather = item("weather");
        DataItem currency = item("currency");
        when(storage.load()).thenReturn(List.of(weather, currency));

        assertThat(service.loadData(null)).containsExactly(weather, currency);
        assertThat(service.loadData("  ")).containsExactly(weather, currency);
        assertThat(service.loadData("weather")).containsExactly(weather);
        assertThat(service.loadData(" currency ")).containsExactly(currency);
        assertThat(service.loadData("missing")).isEmpty();
        verify(storageFactory, times(5)).create(Path.of("data/results.json"), OutputFormat.JSON);
    }

    @Test
    void loadsDataFromSessionStorage() throws StorageException {
        service.start(null);
        when(storage.load()).thenReturn(List.of(item("weather")));

        assertThat(service.loadData(null)).hasSize(1);
        verify(storageFactory, times(1)).create(any(Path.class), any(OutputFormat.class));
    }

    @Test
    void propagatesLoadFailures() throws StorageException {
        when(storage.load()).thenThrow(new StorageException("boom"));

        assertThatThrownBy(() -> service.loadData(null)).isInstanceOf(StorageException.class);
    }

    @Test
    void shutdownStopsRunningSession() {
        service.start(null);

        service.shutdown();

        assertThat(service.status().running()).isFalse();
    }

    @Test
    void shutdownWithoutSessionIsNoOp() {
        assertThatCode(service::shutdown).doesNotThrowAnyException();
    }

    private static Stream<Arguments> startRequests() {
        return Stream.of(
                Arguments.of("request values override configuration",
                        new PollingRequestResponse.SourceRequest("weather", List.of(Map.of("city", "Rome"))),
                        "CSV", "APPEND", "build/custom.csv",
                        List.of(Map.of("city", "Rome")), "csv", "append",
                        Path.of("build/custom.csv").toString(), 1),
                Arguments.of("blank request values fall back to configuration",
                        new PollingRequestResponse.SourceRequest("currency", List.of(Map.of("from", "GBP"))),
                        " ", " ", " ",
                        List.of(Map.of("from", "GBP")), "json", "create",
                        Path.of("data/results.json").toString(), 1),
                Arguments.of("null request params fall back to configured params",
                        new PollingRequestResponse.SourceRequest("weather", null),
                        null, null, null,
                        List.of(Map.of("city", "Berlin"), Map.of("city", "Paris")), "json", "create",
                        Path.of("data/results.json").toString(), 2),
                Arguments.of("empty request params fall back to configured params",
                        new PollingRequestResponse.SourceRequest("currency", List.of()),
                        null, null, null,
                        List.of(Map.of("from", "USD")), "json", "create",
                        Path.of("data/results.json").toString(), 1)
        );
    }

    private AppProperties.Source source(List<Map<String, String>> params) {
        AppProperties.Source source = new AppProperties.Source();
        source.setParams(params);
        return source;
    }

    private DataItem item(String source) {
        return new DataItem(source, Instant.EPOCH, Map.of(), Map.of());
    }
}
