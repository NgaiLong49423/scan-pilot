package com.scanpilot.scanner.pipeline;

import com.scanpilot.scanner.config.SnapshotGuardrailProperties;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Streamed Snapshot Fetcher & Resource Guardrails Tests")
class StreamedSnapshotFetcherTest {

    private SnapshotGuardrailProperties properties;
    private StreamedSnapshotFetcher fetcher;

    @BeforeEach
    void setUp() {
        properties = new SnapshotGuardrailProperties();
        properties.setMaxArchiveBytes(20 * 1024 * 1024L); // 20 MiB
        properties.setMaxWorkspaceBytes(150 * 1024 * 1024L); // 150 MiB
        properties.setMaxEntryCount(10000); // 10,000 entries
        fetcher = new StreamedSnapshotFetcher(properties);
    }

    @Test
    @DisplayName("AC-01: Download exceeding 20 MiB archive ceiling aborts early with REPOSITORY_TOO_LARGE")
    void testDownloadExceeding20MiBAbortsEarly(@TempDir Path workspacePath) throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);

        // Build a zip stream with uncompressed STORED entries totaling > 20 MiB archive size
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            byte[] block = new byte[1024 * 1024]; // 1 MiB block
            java.util.Arrays.fill(block, (byte) 0xAA);
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(block);
            long crcVal = crc.getValue();

            for (int i = 0; i < 21; i++) {
                ZipEntry entry = new ZipEntry("repo-main/chunk_" + i + ".dat");
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(block.length);
                entry.setCompressedSize(block.length);
                entry.setCrc(crcVal);
                zos.putNextEntry(entry);
                zos.write(block);
                zos.closeEntry();
            }
        }

        byte[] archiveBytes = baos.toByteArray();
        assertThat(archiveBytes.length).isGreaterThan(20 * 1024 * 1024);

        when(mockResponse.body()).thenReturn(new ByteArrayInputStream(archiveBytes));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertThatThrownBy(() -> fetcher.downloadAndExtract(mockHttpClient, "https://api.github.com/repos/org/repo/zipball/main", null, workspacePath))
                .isInstanceOf(ResourceGuardrailExceededException.class)
                .satisfies(ex -> {
                    ResourceGuardrailExceededException rge = (ResourceGuardrailExceededException) ex;
                    assertThat(rge.getReasonCode()).isEqualTo("REPOSITORY_TOO_LARGE");
                    assertThat(rge.getLimitHitValue()).isEqualTo(20 * 1024 * 1024L);
                    assertThat(rge.getObservedBytes()).isGreaterThan(20 * 1024 * 1024L);
                });
    }

    @Test
    @DisplayName("AC-02: Workspace extraction exceeding 150 MiB ceiling aborts with REPOSITORY_TOO_LARGE")
    void testExtractionExceeding150MiBAborts(@TempDir Path workspacePath) throws Exception {
        // Build a compressed zip stream that expands to > 150 MiB (e.g. 151 MiB of zeroes compresses to < 200 KB)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("repo-main/large-file.bin");
            zos.putNextEntry(entry);
            byte[] chunk = new byte[64 * 1024]; // 64 KB chunk
            int chunks = (int) ((151 * 1024 * 1024L) / chunk.length) + 1;
            for (int i = 0; i < chunks; i++) {
                zos.write(chunk);
            }
            zos.closeEntry();
        }

        byte[] zipBytes = baos.toByteArray();
        assertThat(zipBytes.length).isLessThan(20 * 1024 * 1024); // Compressed archive is well under 20 MiB

        assertThatThrownBy(() -> fetcher.extractZipStream(new ByteArrayInputStream(zipBytes), workspacePath))
                .isInstanceOf(ResourceGuardrailExceededException.class)
                .satisfies(ex -> {
                    ResourceGuardrailExceededException rge = (ResourceGuardrailExceededException) ex;
                    assertThat(rge.getReasonCode()).isEqualTo("REPOSITORY_TOO_LARGE");
                    assertThat(rge.getLimitHitValue()).isEqualTo(150 * 1024 * 1024L);
                    assertThat(rge.getObservedBytes()).isGreaterThan(150 * 1024 * 1024L);
                });
    }

    @Test
    @DisplayName("AC-03: Archive containing more than 10,000 entries aborts with TOO_MANY_FILES (zip-bomb defense)")
    void testExtractionExceeding10000EntriesAborts(@TempDir Path workspacePath) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Write 10,001 entries
            for (int i = 0; i <= 10000; i++) {
                ZipEntry entry = new ZipEntry("repo-main/file_" + i + ".txt");
                zos.putNextEntry(entry);
                zos.write("x".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        byte[] zipBytes = baos.toByteArray();

        assertThatThrownBy(() -> fetcher.extractZipStream(new ByteArrayInputStream(zipBytes), workspacePath))
                .isInstanceOf(ResourceGuardrailExceededException.class)
                .satisfies(ex -> {
                    ResourceGuardrailExceededException rge = (ResourceGuardrailExceededException) ex;
                    assertThat(rge.getReasonCode()).isEqualTo("TOO_MANY_FILES");
                    assertThat(rge.getLimitHitValue()).isEqualTo(10000L);
                    assertThat(rge.getObservedFiles()).isEqualTo(10001);
                });
    }

    @Test
    @DisplayName("AC-05: Dual-slash Zip-Slip and absolute path traversal vectors are blocked and discarded")
    void testZipSlipUnixAndWindowsTraversalsBlocked(@TempDir Path tempDir) throws Exception {
        Path workspacePath = tempDir.resolve("workspace");
        Files.createDirectories(workspacePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Valid entry
            zos.putNextEntry(new ZipEntry("repo-main/src/App.java"));
            zos.write("public class App {}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Unix traversal vectors
            zos.putNextEntry(new ZipEntry("repo-main/../evil.txt"));
            zos.write("malicious unix content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("repo-main/../../unix-evil.txt"));
            zos.write("malicious unix content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Windows traversal vectors with backslashes
            zos.putNextEntry(new ZipEntry("..\\evil.txt"));
            zos.write("malicious raw backslash content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("repo-main\\..\\evil.txt"));
            zos.write("malicious win backslash content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("repo-main\\..\\..\\win-evil.txt"));
            zos.write("malicious win content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Absolute path vectors
            zos.putNextEntry(new ZipEntry("/tmp/abs.txt"));
            zos.write("malicious abs content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("/tmp/abs-evil.txt"));
            zos.write("malicious abs content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("C:/windows/abs.txt"));
            zos.write("malicious drive content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        fetcher.extractZipStream(new ByteArrayInputStream(baos.toByteArray()), workspacePath);

        // Valid file extracted safely
        assertThat(Files.exists(workspacePath.resolve("src/App.java"))).isTrue();

        // Assert all traversal/absolute entries rejected and NO file created inside or outside workspace
        assertThat(Files.exists(workspacePath.resolve("evil.txt"))).isFalse();
        assertThat(Files.exists(workspacePath.resolve("abs.txt"))).isFalse();
        assertThat(Files.exists(tempDir.resolve("evil.txt"))).isFalse();
        assertThat(Files.exists(tempDir.resolve("unix-evil.txt"))).isFalse();
        assertThat(Files.exists(tempDir.resolve("win-evil.txt"))).isFalse();
        assertThat(Files.exists(Path.of("/tmp/abs.txt"))).isFalse();
        assertThat(Files.exists(Path.of("/tmp/abs-evil.txt"))).isFalse();
        assertThat(Files.exists(Path.of("C:/windows/abs.txt"))).isFalse();
    }

    @Test
    @DisplayName("Non-2xx HTTP response releases stream resources and closes InputStream without leaking")
    void testNon2xxHttpResponseClosesStreamWithoutLeaking(@TempDir Path workspacePath) throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
        InputStream mockInputStream = mock(InputStream.class);

        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.body()).thenReturn(mockInputStream);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertThatThrownBy(() -> fetcher.downloadAndExtract(mockHttpClient, "https://api.github.com/repos/org/repo/zipball/main", "ghp_secret_token_1234567890", workspacePath))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("GitHub snapshot archive download failed with HTTP 404")
                .satisfies(ex -> {
                    assertThat(ex.getMessage()).contains("HTTP 404");
                    assertThat(ex.getMessage()).doesNotContain("Not Found");
                    assertThat(ex.getMessage()).doesNotContain("ghp_secret_token_1234567890");
                });

        verify(mockInputStream, times(1)).close();
    }

    @Test
    @DisplayName("R67-09: Extraction aborts with SCAN_TIMEOUT when job deadline expires during zip entry processing")
    void testExtractionAbortsWhenJobDeadlineExpiresDuringZipProcessing(@TempDir Path workspacePath) throws Exception {
        // Build a multi-entry zip fixture
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < 50; i++) {
                ZipEntry entry = new ZipEntry("repo-main/file_" + i + ".txt");
                zos.putNextEntry(entry);
                zos.write(("Sample file content " + i).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        byte[] zipBytes = baos.toByteArray();

        // Slow InputStream that sleeps slightly on reads so deadline expires during processing
        InputStream slowIs = new InputStream() {
            private final ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);

            @Override
            public int read() throws IOException {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return bais.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return bais.read(b, off, len);
            }
        };

        Instant deadline = Instant.now().plusMillis(30);

        assertThatThrownBy(() -> fetcher.extractZipStream(slowIs, workspacePath, deadline))
                .isInstanceOf(ResourceGuardrailExceededException.class)
                .satisfies(ex -> {
                    ResourceGuardrailExceededException rge = (ResourceGuardrailExceededException) ex;
                    assertThat(rge.getReasonCode()).isEqualTo("SCAN_TIMEOUT");
                    assertThat(rge.getLimitHitValue()).isEqualTo(180L);
                });
    }

    @Test
    @DisplayName("R67-09: Download aborts early with SCAN_TIMEOUT if job deadline is already expired")
    void testDownloadAbortsEarlyIfJobDeadlineAlreadyExpired(@TempDir Path workspacePath) {
        HttpClient mockHttpClient = mock(HttpClient.class);
        Instant expiredDeadline = Instant.now().minusSeconds(1);

        assertThatThrownBy(() -> fetcher.downloadAndExtract(
                mockHttpClient,
                "https://api.github.com/repos/org/repo/zipball/main",
                null,
                workspacePath,
                expiredDeadline
        ))
                .isInstanceOf(ResourceGuardrailExceededException.class)
                .satisfies(ex -> {
                    ResourceGuardrailExceededException rge = (ResourceGuardrailExceededException) ex;
                    assertThat(rge.getReasonCode()).isEqualTo("SCAN_TIMEOUT");
                    assertThat(rge.getLimitHitValue()).isEqualTo(180L);
                });
    }
}
