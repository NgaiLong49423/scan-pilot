package com.scanpilot.system;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Root controller serving welcome API metadata at "/" to prevent 404 Whitelabel errors.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getRoot() {
        return ResponseEntity.ok(Map.of(
                "application", "Scan Pilot REST API",
                "status", "HEALTHY",
                "version", "v1.0.0",
                "documentation", "https://github.com/NgaiLong49423/scan-pilot",
                "frontend", "https://scan-pilot.ai.studio",
                "timestamp", Instant.now().toString()
        ));
    }
}
