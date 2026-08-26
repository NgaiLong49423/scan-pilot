package com.scanpilot.scanner.detector.gitleaks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GitleaksConfigProperties Unit Tests")
class GitleaksConfigPropertiesTest {

    @Test
    @DisplayName("R67-01: Default production watchdog timeout is 180 seconds")
    void testDefaultTimeoutSecondsIs180() {
        GitleaksConfigProperties properties = new GitleaksConfigProperties();
        assertThat(properties.getTimeoutSeconds()).isEqualTo(180);
    }
}
