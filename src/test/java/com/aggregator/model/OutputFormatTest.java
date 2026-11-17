package com.aggregator.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutputFormatTest {

    @Test
    void parsesFormatsIgnoringCaseAndWhitespace() {
        assertThat(OutputFormat.fromString(" csv ")).isEqualTo(OutputFormat.CSV);
        assertThat(OutputFormat.fromString("Json")).isEqualTo(OutputFormat.JSON);
    }

    @Test
    void rejectsUnknownFormat() {
        assertThatThrownBy(() -> OutputFormat.fromString("xml"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
