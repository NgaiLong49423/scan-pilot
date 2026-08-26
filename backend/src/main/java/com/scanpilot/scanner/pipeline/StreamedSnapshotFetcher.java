package com.scanpilot.scanner.pipeline;

import com.scanpilot.scanner.config.SnapshotGuardrailProperties;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Streaming fetcher and extractor for repository snapshot archives with strict resource guardrails (FR-028, FR-031).
 * Enforces download byte limits (20 MiB), cumulative workspace extraction limits (150 MiB),
 * entry count limits (10,000), and dual-slash Zip-Slip path traversal protection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamedSnapshotFetcher {

    private final SnapshotGuardrailProperties properties;

    /**
     * Downloads a snapshot archive from remote GitHub URL via HTTP streaming and extracts it directly
     * into workspacePath under strict guardrails.
     *
     * @param httpClient    HTTP client instance
     * @param url           Remote snapshot URL (e.g. GitHub zipball endpoint)
     * @param token         Optional authorization token
     * @param workspacePath Target directory for extracted repository files
     */
    public void downloadAndExtract(HttpClient httpClient, String url, String token, Path workspacePath) {
        downloadAndExtract(httpClient, url, token, workspacePath, null);
    }

    /**
     * Downloads a snapshot archive from remote GitHub URL via HTTP streaming and extracts it directly
     * into workspacePath under strict guardrails and overarching job deadline.
     *
     * @param httpClient    HTTP client instance
     * @param url           Remote snapshot URL (e.g. GitHub zipball endpoint)
     * @param token         Optional authorization token
     * @param workspacePath Target directory for extracted repository files
     * @param jobDeadline   Overarching whole-job deadline
     */
    public void downloadAndExtract(HttpClient httpClient, String url, String token, Path workspacePath, Instant jobDeadline) {
        if (httpClient == null) {
            throw new IllegalArgumentException("HttpClient must not be null");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Snapshot URL must not be blank");
        }
        if (workspacePath == null) {
            throw new IllegalArgumentException("Workspace path must not be null");
        }

        Duration requestTimeout = Duration.ofMinutes(2);
        if (jobDeadline != null) {
            long remainingSeconds = Duration.between(Instant.now(), jobDeadline).toSeconds();
            if (remainingSeconds <= 0 || Instant.now().isAfter(jobDeadline)) {
                throw new ResourceGuardrailExceededException(
                        "SCAN_TIMEOUT",
                        0,
                        0,
                        properties.getMaxScanTimeoutSeconds()
                );
            }
            requestTimeout = Duration.ofSeconds(Math.min(120, Math.max(1, remainingSeconds)));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Scan-Pilot")
                .timeout(requestTimeout)
                .GET();

        if (token != null && !token.isBlank() && !token.startsWith("mock-")) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Snapshot download interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to connect to snapshot endpoint: " + sanitizeError(e.getMessage()), e);
        }

        int statusCode = response.statusCode();
        try (InputStream is = response.body()) {
            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("GitHub snapshot archive download failed with HTTP " + statusCode);
            }
            extractZipStream(is, workspacePath, jobDeadline);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to process snapshot stream: " + sanitizeError(e.getMessage()), e);
        }
    }

    /**
     * Extracts a zip input stream into workspacePath while enforcing all resource guardrails:
     * 1. Bounded archive stream size (maxArchiveBytes, default 20 MiB).
     * 2. Bounded entry count (maxEntryCount, default 10,000 entries).
     * 3. Bounded cumulative extracted workspace size (maxWorkspaceBytes, default 150 MiB).
     * 4. Multi-platform Zip-Slip canonicalization and rejection.
     *
     * @param inputStream   Raw archive input stream
     * @param workspacePath Target directory for extracted repository files
     * @throws ResourceGuardrailExceededException if any safety threshold is breached
     */
    public void extractZipStream(InputStream inputStream, Path workspacePath) {
        extractZipStream(inputStream, workspacePath, null);
    }

    /**
     * Extracts a zip input stream into workspacePath while enforcing all resource guardrails and whole-job deadline:
     * 1. Bounded archive stream size (maxArchiveBytes, default 20 MiB).
     * 2. Bounded entry count (maxEntryCount, default 10,000 entries).
     * 3. Bounded cumulative extracted workspace size (maxWorkspaceBytes, default 150 MiB).
     * 4. Multi-platform Zip-Slip canonicalization and rejection.
     * 5. Whole scan job deadline enforcement across entries and buffer reads.
     *
     * @param inputStream   Raw archive input stream
     * @param workspacePath Target directory for extracted repository files
     * @param jobDeadline   Overarching whole-job deadline
     * @throws ResourceGuardrailExceededException if any safety threshold is breached
     */
    public void extractZipStream(InputStream inputStream, Path workspacePath, Instant jobDeadline) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }
        if (workspacePath == null) {
            throw new IllegalArgumentException("Workspace path must not be null");
        }

        long maxArchiveBytes = properties.getMaxArchiveBytes();
        long maxWorkspaceBytes = properties.getMaxWorkspaceBytes();
        int maxEntryCount = properties.getMaxEntryCount();

        CountingInputStream countingIs = new CountingInputStream(inputStream, maxArchiveBytes);
        int entryCount = 0;
        long extractedBytes = 0;

        try (ZipInputStream zis = new ZipInputStream(countingIs)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                if (jobDeadline != null && Instant.now().isAfter(jobDeadline)) {
                    throw new ResourceGuardrailExceededException(
                            "SCAN_TIMEOUT",
                            extractedBytes,
                            entryCount,
                            properties.getMaxScanTimeoutSeconds()
                    );
                }

                entryCount++;
                if (entryCount > maxEntryCount) {
                    throw new ResourceGuardrailExceededException(
                            "TOO_MANY_FILES",
                            extractedBytes,
                            entryCount,
                            maxEntryCount
                    );
                }

                String rawName = entry.getName();
                if (rawName == null || rawName.isBlank()) {
                    zis.closeEntry();
                    continue;
                }

                String normalized = rawName.replace('\\', '/');

                // Reject dangerous path traversal formats (absolute paths, drive specifiers, UNC, or .. traversal segments)
                if (normalized.startsWith("/")
                        || normalized.contains(":")
                        || rawName.startsWith("\\")
                        || Arrays.stream(normalized.split("/")).anyMatch(p -> p.equals(".."))) {
                    log.warn("Archive entry rejected due to path traversal or absolute path vector");
                    zis.closeEntry();
                    continue;
                }

                // Strip top-level directory prefix if archive contains GitHub wrapper folder
                int slashIdx = normalized.indexOf('/');
                String relativePath = slashIdx >= 0 ? normalized.substring(slashIdx + 1) : normalized;

                if (relativePath.isBlank()) {
                    zis.closeEntry();
                    continue;
                }

                Path normalizedWorkspace = workspacePath.toAbsolutePath().normalize();
                Path destination = normalizedWorkspace.resolve(relativePath).normalize();

                // Multi-platform Zip-Slip traversal defense contract
                if (!destination.startsWith(normalizedWorkspace) || destination.equals(normalizedWorkspace)) {
                    log.warn("Archive entry rejected due to path traversal or absolute path vector");
                    zis.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    if (destination.getParent() != null) {
                        Files.createDirectories(destination.getParent());
                    }

                    try (OutputStream fos = Files.newOutputStream(
                            destination,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            if (jobDeadline != null && Instant.now().isAfter(jobDeadline)) {
                                throw new ResourceGuardrailExceededException(
                                        "SCAN_TIMEOUT",
                                        extractedBytes,
                                        entryCount,
                                        properties.getMaxScanTimeoutSeconds()
                                );
                            }
                            extractedBytes += len;
                            if (extractedBytes > maxWorkspaceBytes) {
                                throw new ResourceGuardrailExceededException(
                                        "REPOSITORY_TOO_LARGE",
                                        extractedBytes,
                                        entryCount,
                                        maxWorkspaceBytes
                                );
                            }
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (ResourceGuardrailExceededException rge) {
            throw rge;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract snapshot archive: " + sanitizeError(e.getMessage()), e);
        }
    }

    private String sanitizeError(String message) {
        if (message == null || message.isBlank()) return "Unknown error";
        return message.replaceAll("(?i)(gh[pousr]_[A-Za-z0-9_]{16,})", "[REDACTED_TOKEN]")
                .replaceAll("(?i)(bearer\\s+)[A-Za-z0-9_.-]+", "$1[REDACTED_TOKEN]");
    }

    /**
     * Bounded CountingInputStream that throws ResourceGuardrailExceededException immediately
     * when archive download stream exceeds maxArchiveBytes.
     */
    private static class CountingInputStream extends FilterInputStream {
        private final long maxBytes;
        private long bytesRead = 0;

        public CountingInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                checkLimit(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result > 0) {
                checkLimit(result);
            }
            return result;
        }

        private void checkLimit(long additionalBytes) {
            bytesRead += additionalBytes;
            if (bytesRead > maxBytes) {
                throw new ResourceGuardrailExceededException(
                        "REPOSITORY_TOO_LARGE",
                        bytesRead,
                        0,
                        maxBytes
                );
            }
        }
    }
}
