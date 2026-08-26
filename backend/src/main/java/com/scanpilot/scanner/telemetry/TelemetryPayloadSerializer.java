package com.scanpilot.scanner.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class TelemetryPayloadSerializer {

    private static final int MAX_BYTE_LENGTH = 1024;
    private final ObjectMapper objectMapper;

    public TelemetryPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public String serialize(ScanEventPayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_BYTE_LENGTH) {
                log.warn("Telemetry payload exceeded maximum allowed length ({} bytes), suppressing", bytes.length);
                return null;
            }
            return json;
        } catch (Exception e) {
            log.warn("Failed to serialize telemetry payload");
            return null;
        }
    }
}
