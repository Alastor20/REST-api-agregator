package com.aggregator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThatCode;

class AggregatorApplicationTest {

    @Test
    @Timeout(30)
    void mainStartsApplicationWithoutWebServer() {
        assertThatCode(() -> AggregatorApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.main.banner-mode=off",
                "--logging.level.root=OFF"
        })).doesNotThrowAnyException();
    }
}
