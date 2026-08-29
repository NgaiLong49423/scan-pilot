package com.scanpilot.github.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BoundedStreamReader Unit Tests")
class BoundedStreamReaderTest {

    @Test
    @DisplayName("Should successfully read payload within limit")
    void testReadsStreamWithinLimit() throws IOException {
        byte[] data = "{\"action\":\"opened\"}".getBytes();
        InputStream is = new ByteArrayInputStream(data);

        byte[] result = BoundedStreamReader.readBoundedStream(is, 1024);

        assertThat(result).isEqualTo(data);
    }

    @Test
    @DisplayName("Should successfully read payload exactly at limit")
    void testReadsStreamExactlyAtLimit() throws IOException {
        byte[] data = new byte[1024];
        Arrays.fill(data, (byte) 'a');
        InputStream is = new ByteArrayInputStream(data);

        byte[] result = BoundedStreamReader.readBoundedStream(is, 1024);

        assertThat(result).hasSize(1024);
        assertThat(result).isEqualTo(data);
    }

    @Test
    @DisplayName("Should throw PayloadTooLargeException when stream exceeds 1 MiB limit")
    void testRejectsStreamExceeding1MiBWithoutContentLength() {
        int limit = 1024;
        byte[] oversizedData = new byte[limit + 1];
        Arrays.fill(oversizedData, (byte) 'x');
        InputStream is = new ByteArrayInputStream(oversizedData);

        assertThatThrownBy(() -> BoundedStreamReader.readBoundedStream(is, limit))
                .isInstanceOf(BoundedStreamReader.PayloadTooLargeException.class)
                .hasMessageContaining("Payload exceeds maximum permitted size");
    }

    @Test
    @DisplayName("Should throw PayloadTooLargeException when stream exceeds physical 1 MiB limit")
    void testRejectsStreamExceedingPhysical1MiBLimit() {
        int maxBytes = 1_048_576; // 1 MiB
        byte[] oversizedData = new byte[maxBytes + 1];
        Arrays.fill(oversizedData, (byte) 'x');
        InputStream is = new ByteArrayInputStream(oversizedData);

        assertThatThrownBy(() -> BoundedStreamReader.readBoundedStream(is, maxBytes))
                .isInstanceOf(BoundedStreamReader.PayloadTooLargeException.class)
                .hasMessageContaining("Payload exceeds maximum permitted size");
    }

    @Test
    @DisplayName("Should safely handle empty and null streams")
    void testHandlesEmptyAndNullStreams() throws IOException {
        byte[] emptyResult = BoundedStreamReader.readBoundedStream(new ByteArrayInputStream(new byte[0]), 1024);
        assertThat(emptyResult).isEmpty();

        byte[] nullResult = BoundedStreamReader.readBoundedStream(null, 1024);
        assertThat(nullResult).isEmpty();
    }
}
