package com.scanpilot.scanner.classifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Service for layered content classification of repository files.
 * Performs fast, memory-safe byte sampling (up to 8KB) and signature detection.
 */
@Slf4j
@Service
public class ContentClassifierService {

    public static final int SAMPLE_SIZE_BYTES = 8192; // 8 KiB buffer

    private static final Map<String, String> EXTENSION_MIME_MAP = Map.ofEntries(
        Map.entry(".java", "text/x-java-source"),
        Map.entry(".kt", "text/x-kotlin"),
        Map.entry(".py", "text/x-python"),
        Map.entry(".js", "application/javascript"),
        Map.entry(".jsx", "text/jsx"),
        Map.entry(".ts", "application/typescript"),
        Map.entry(".tsx", "text/tsx"),
        Map.entry(".json", "application/json"),
        Map.entry(".yaml", "text/yaml"),
        Map.entry(".yml", "text/yaml"),
        Map.entry(".xml", "text/xml"),
        Map.entry(".html", "text/html"),
        Map.entry(".htm", "text/html"),
        Map.entry(".css", "text/css"),
        Map.entry(".scss", "text/x-scss"),
        Map.entry(".md", "text/markdown"),
        Map.entry(".txt", "text/plain"),
        Map.entry(".properties", "text/x-java-properties"),
        Map.entry(".toml", "application/toml"),
        Map.entry(".ini", "text/plain"),
        Map.entry(".env", "text/plain"),
        Map.entry(".sh", "application/x-sh"),
        Map.entry(".bash", "application/x-sh"),
        Map.entry(".zsh", "application/x-sh"),
        Map.entry(".sql", "application/sql"),
        Map.entry(".csv", "text/csv"),
        Map.entry(".tsv", "text/tab-separated-values"),
        Map.entry(".go", "text/x-go"),
        Map.entry(".rs", "text/x-rust"),
        Map.entry(".c", "text/x-c"),
        Map.entry(".cpp", "text/x-c++"),
        Map.entry(".h", "text/x-c-header"),
        Map.entry(".hpp", "text/x-c++-header"),
        Map.entry(".cs", "text/x-csharp"),
        Map.entry(".php", "text/x-php"),
        Map.entry(".rb", "text/x-ruby"),
        Map.entry(".gradle", "text/x-groovy"),
        Map.entry(".groovy", "text/x-groovy"),
        Map.entry(".tf", "text/x-terraform"),
        Map.entry(".proto", "text/x-protobuf")
    );

    /**
     * Classifies content from a file path on disk.
     */
    public ClassificationResult classify(Path path) throws IOException {
        String filename = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        if (!Files.exists(path)) {
            return ClassificationResult.undetermined("File does not exist: " + path);
        }
        if (Files.isDirectory(path)) {
            return ClassificationResult.undetermined("Path is a directory: " + path);
        }

        long fileSize = Files.size(path);
        if (fileSize == 0) {
            return ClassificationResult.text("text/plain", 1.0, "Empty file (0 bytes)");
        }

        byte[] sample = new byte[(int) Math.min(fileSize, SAMPLE_SIZE_BYTES)];
        try (InputStream in = Files.newInputStream(path)) {
            int bytesRead = in.readNBytes(sample, 0, sample.length);
            if (bytesRead < sample.length) {
                sample = Arrays.copyOf(sample, bytesRead);
            }
        }
        return classify(filename, sample);
    }

    /**
     * Classifies content from an InputStream reading up to 8KB.
     */
    public ClassificationResult classify(String path, InputStream inputStream) throws IOException {
        byte[] sample = inputStream.readNBytes(SAMPLE_SIZE_BYTES);
        return classify(path, sample);
    }

    /**
     * Classifies content from a sampled byte array and file path.
     */
    public ClassificationResult classify(String path, byte[] sample) {
        if (sample == null || sample.length == 0) {
            return ClassificationResult.text("text/plain", 1.0, "Empty file (0 bytes)");
        }

        String lowerPath = path != null ? path.toLowerCase(Locale.ROOT) : "";

        // 1. Magic byte & signature recognition
        ClassificationResult magicResult = checkMagicSignatures(lowerPath, sample);
        if (magicResult != null) {
            return magicResult;
        }

        // 2. Byte sampling analysis: Detect null bytes
        if (containsNullByte(sample)) {
            return ClassificationResult.binary(
                "application/octet-stream",
                false,
                0.99,
                "Binary data detected (contains null bytes 0x00)"
            );
        }

        // 3. UTF-8 decodability & printable character heuristics
        return evaluateTextSample(lowerPath, sample);
    }

    private ClassificationResult checkMagicSignatures(String lowerPath, byte[] sample) {
        int len = sample.length;

        // PDF: starts with "%PDF"
        if (startsWith(sample, (byte) '%', (byte) 'P', (byte) 'D', (byte) 'F')) {
            return ClassificationResult.binary("application/pdf", true, 1.0, "PDF document signature (%PDF)");
        }

        // MS Office OLE Compound Document (DOC, XLS, PPT)
        if (startsWith(sample, (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1)) {
            String mime = "application/x-ole-storage";
            if (lowerPath.endsWith(".doc")) mime = "application/msword";
            else if (lowerPath.endsWith(".xls")) mime = "application/vnd.ms-excel";
            else if (lowerPath.endsWith(".ppt")) mime = "application/vnd.ms-powerpoint";
            return ClassificationResult.binary(mime, true, 1.0, "Microsoft Office OLE document signature");
        }

        // ZIP Archives (PK\x03\x04, PK\x05\x06, PK\x07\x08)
        if (startsWith(sample, (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04)
            || startsWith(sample, (byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06)
            || startsWith(sample, (byte) 0x50, (byte) 0x4B, (byte) 0x07, (byte) 0x08)) {
            
            // Check for Office Open XML (DOCX, XLSX, PPTX)
            if (lowerPath.endsWith(".docx") || lowerPath.endsWith(".dotx")) {
                return ClassificationResult.binary("application/vnd.openxmlformats-officedocument.wordprocessingml.document", true, 1.0, "DOCX Office document signature");
            }
            if (lowerPath.endsWith(".xlsx") || lowerPath.endsWith(".xltx")) {
                return ClassificationResult.binary("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", true, 1.0, "XLSX Office document signature");
            }
            if (lowerPath.endsWith(".pptx") || lowerPath.endsWith(".ppsx")) {
                return ClassificationResult.binary("application/vnd.openxmlformats-officedocument.presentationml.presentation", true, 1.0, "PPTX Office document signature");
            }

            // Inspect zip sample content for OOXML indicators
            if (containsSequence(sample, "[Content_Types].xml") || containsSequence(sample, "word/") || containsSequence(sample, "xl/") || containsSequence(sample, "ppt/")) {
                return ClassificationResult.binary("application/vnd.openxmlformats-officedocument", true, 0.95, "Office Open XML document signature");
            }

            return ClassificationResult.binary("application/zip", false, 1.0, "ZIP archive signature");
        }

        // ELF Executable: \x7fELF
        if (startsWith(sample, (byte) 0x7F, (byte) 'E', (byte) 'L', (byte) 'F')) {
            return ClassificationResult.binary("application/x-elf", false, 1.0, "ELF executable signature");
        }

        // Windows PE / DOS Executable: MZ
        if (startsWith(sample, (byte) 'M', (byte) 'Z')) {
            return ClassificationResult.binary("application/vnd.microsoft.portable-executable", false, 1.0, "Windows PE/DOS executable signature");
        }

        // Mach-O Binaries
        if (startsWith(sample, (byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE)
            || startsWith(sample, (byte) 0xCE, (byte) 0xFA, (byte) 0xED, (byte) 0xFE)
            || startsWith(sample, (byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCF)
            || startsWith(sample, (byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE)) {
            return ClassificationResult.binary("application/x-mach-binary", false, 1.0, "Mach-O binary signature");
        }

        // Java Class File / Mach-O Fat Binary: 0xCAFEBABE
        if (startsWith(sample, (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE)) {
            return ClassificationResult.binary("application/java-vm", false, 1.0, "Java class / Universal binary signature");
        }

        // PNG: \x89PNG\r\n\x1a\n
        if (startsWith(sample, (byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A)) {
            return ClassificationResult.binary("image/png", false, 1.0, "PNG image signature");
        }

        // JPEG: 0xFF, 0xD8, 0xFF
        if (startsWith(sample, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) {
            return ClassificationResult.binary("image/jpeg", false, 1.0, "JPEG image signature");
        }

        // GIF: GIF87a or GIF89a
        if (startsWith(sample, (byte) 'G', (byte) 'I', (byte) 'F', (byte) '8', (byte) '7', (byte) 'a')
            || startsWith(sample, (byte) 'G', (byte) 'I', (byte) 'F', (byte) '8', (byte) '9', (byte) 'a')) {
            return ClassificationResult.binary("image/gif", false, 1.0, "GIF image signature");
        }

        // WebP: RIFF....WEBP
        if (len >= 12 && startsWith(sample, (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F')
            && sample[8] == 'W' && sample[9] == 'E' && sample[10] == 'B' && sample[11] == 'P') {
            return ClassificationResult.binary("image/webp", false, 1.0, "WebP image signature");
        }

        // BMP: BM
        if (len >= 14 && startsWith(sample, (byte) 'B', (byte) 'M')) {
            return ClassificationResult.binary("image/bmp", false, 0.95, "BMP image signature");
        }

        // TIFF: II*\0 or MM\0*
        if (startsWith(sample, (byte) 0x49, (byte) 0x49, (byte) 0x2A, (byte) 0x00)
            || startsWith(sample, (byte) 0x4D, (byte) 0x4D, (byte) 0x00, (byte) 0x2A)) {
            return ClassificationResult.binary("image/tiff", false, 1.0, "TIFF image signature");
        }

        // GZIP: 0x1F, 0x8B
        if (startsWith(sample, (byte) 0x1F, (byte) 0x8B)) {
            return ClassificationResult.binary("application/gzip", false, 1.0, "GZIP archive signature");
        }

        // BZIP2: BZh
        if (startsWith(sample, (byte) 'B', (byte) 'Z', (byte) 'h')) {
            return ClassificationResult.binary("application/x-bzip2", false, 1.0, "BZIP2 archive signature");
        }

        // 7-Zip: 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C
        if (startsWith(sample, (byte) 0x37, (byte) 0x7A, (byte) 0xBC, (byte) 0xAF, (byte) 0x27, (byte) 0x1C)) {
            return ClassificationResult.binary("application/x-7z-compressed", false, 1.0, "7-Zip archive signature");
        }

        // WebAssembly: \0asm
        if (startsWith(sample, (byte) 0x00, (byte) 'a', (byte) 's', (byte) 'm')) {
            return ClassificationResult.binary("application/wasm", false, 1.0, "WebAssembly binary signature");
        }

        // SQLite DB
        if (startsWith(sample, (byte) 'S', (byte) 'Q', (byte) 'L', (byte) 'i', (byte) 't', (byte) 'e', (byte) ' ', (byte) 'f', (byte) 'o', (byte) 'r', (byte) 'm', (byte) 'a', (byte) 't', (byte) ' ', (byte) '3', (byte) 0x00)) {
            return ClassificationResult.binary("application/vnd.sqlite3", false, 1.0, "SQLite database signature");
        }

        return null;
    }

    private ClassificationResult evaluateTextSample(String lowerPath, byte[] sample) {
        // Strip UTF-8 BOM if present
        int offset = 0;
        if (sample.length >= 3
            && (sample[0] & 0xFF) == 0xEF
            && (sample[1] & 0xFF) == 0xBB
            && (sample[2] & 0xFF) == 0xBF) {
            offset = 3;
        }

        byte[] contentToAnalyze = offset == 0 ? sample : Arrays.copyOfRange(sample, offset, sample.length);
        if (contentToAnalyze.length == 0) {
            return ClassificationResult.text("text/plain", 1.0, "Empty UTF-8 BOM file");
        }

        // Attempt strict UTF-8 decoding
        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            String decoded = utf8Decoder.decode(ByteBuffer.wrap(contentToAnalyze)).toString();
            return evaluateDecodedString(lowerPath, decoded, "UTF-8");
        } catch (CharacterCodingException e) {
            // UTF-8 decoding failed; try ISO-8859-1 fallback check
            return evaluateIso8859Fallback(lowerPath, contentToAnalyze);
        }
    }

    private ClassificationResult evaluateDecodedString(String lowerPath, String text, String encoding) {
        int length = text.length();
        if (length == 0) {
            return ClassificationResult.text("text/plain", 1.0, "Decoded empty text");
        }

        int printableCount = 0;
        int controlCount = 0;

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (isPrintableOrWhitespace(c)) {
                printableCount++;
            } else {
                controlCount++;
            }
        }

        double printableRatio = (double) printableCount / length;
        double controlRatio = (double) controlCount / length;

        if (controlRatio > 0.05 || printableRatio < 0.90) {
            return ClassificationResult.binary(
                "application/octet-stream",
                false,
                0.85,
                "High unprintable character ratio (" + String.format(Locale.ROOT, "%.2f", controlRatio) + ")"
            );
        }

        String mimeHint = resolveMimeType(lowerPath);
        return ClassificationResult.text(
            mimeHint,
            0.99,
            "Valid " + encoding + " text content (printable ratio: " + String.format(Locale.ROOT, "%.2f", printableRatio) + ")"
        );
    }

    private ClassificationResult evaluateIso8859Fallback(String lowerPath, byte[] content) {
        int length = content.length;
        int printableCount = 0;
        int controlCount = 0;

        for (byte b : content) {
            int unsigned = b & 0xFF;
            if (unsigned == 0x00) {
                return ClassificationResult.binary("application/octet-stream", false, 0.99, "Contains null bytes (0x00)");
            }
            if (isPrintableOrWhitespaceByte(unsigned)) {
                printableCount++;
            } else if (unsigned < 0x20 || unsigned == 0x7F) {
                controlCount++;
            }
        }

        double printableRatio = (double) printableCount / length;
        double controlRatio = (double) controlCount / length;

        if (printableRatio >= 0.90 && controlRatio <= 0.05) {
            return ClassificationResult.text(
                "text/plain; charset=ISO-8859-1",
                0.90,
                "Valid ISO-8859-1 text content (printable ratio: " + String.format(Locale.ROOT, "%.2f", printableRatio) + ")"
            );
        }

        if (controlRatio > 0.10) {
            return ClassificationResult.binary(
                "application/octet-stream",
                false,
                0.85,
                "High non-printable byte ratio in non-UTF-8 stream"
            );
        }

        return ClassificationResult.undetermined(
            "Ambiguous byte distribution, cannot conclusively determine TEXT or BINARY"
        );
    }

    private boolean isPrintableOrWhitespace(char c) {
        if (c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\b' || c == 0x1B) { // 0x1B is ESC (ANSI escapes)
            return true;
        }
        if (c >= 32 && c <= 126) {
            return true;
        }
        return Character.isLetterOrDigit(c) || Character.isSpaceChar(c) || Character.isWhitespace(c)
            || Character.isLetter(c) || !Character.isISOControl(c);
    }

    private boolean isPrintableOrWhitespaceByte(int unsignedByte) {
        if (unsignedByte == 0x09 || unsignedByte == 0x0A || unsignedByte == 0x0D || unsignedByte == 0x0C || unsignedByte == 0x08 || unsignedByte == 0x1B) {
            return true;
        }
        if (unsignedByte >= 0x20 && unsignedByte <= 0x7E) {
            return true;
        }
        return unsignedByte >= 0xA0; // ISO-8859-1 printable range
    }

    private String resolveMimeType(String lowerPath) {
        if (lowerPath == null || lowerPath.isEmpty()) {
            return "text/plain";
        }
        for (Map.Entry<String, String> entry : EXTENSION_MIME_MAP.entrySet()) {
            if (lowerPath.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "text/plain";
    }

    private boolean containsNullByte(byte[] sample) {
        for (byte b : sample) {
            if (b == 0x00) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWith(byte[] sample, byte... prefix) {
        if (sample.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (sample[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean containsSequence(byte[] sample, String text) {
        byte[] target = text.getBytes(StandardCharsets.US_ASCII);
        if (sample.length < target.length) {
            return false;
        }
        for (int i = 0; i <= sample.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (sample[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }
}
