package com.scanpilot.scanner.classifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ContentClassifierServiceTest {

    private ContentClassifierService classifierService;

    @BeforeEach
    void setUp() {
        classifierService = new ContentClassifierService();
    }

    @Nested
    @DisplayName("Text Content Classification")
    class TextContentTests {

        @Test
        @DisplayName("Classifies Java source code as TEXT")
        void testJavaSourceCode() {
            String javaCode = """
                package com.example;
                public class App {
                    public static void main(String[] args) {
                        System.out.println("Hello World!");
                    }
                }
                """;
            ClassificationResult result = classifierService.classify("src/App.java", javaCode.getBytes(StandardCharsets.UTF_8));
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.mimeTypeHint()).isEqualTo("text/x-java-source");
            assertThat(result.isBinaryDocument()).isFalse();
            assertThat(result.confidence()).isGreaterThan(0.9);
        }

        @Test
        @DisplayName("Classifies YAML configuration as TEXT")
        void testYamlConfig() {
            String yaml = """
                server:
                  port: 8080
                spring:
                  application:
                    name: scan-pilot
                """;
            ClassificationResult result = classifierService.classify("application.yml", yaml.getBytes(StandardCharsets.UTF_8));
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.mimeTypeHint()).isEqualTo("text/yaml");
            assertThat(result.isBinaryDocument()).isFalse();
        }

        @Test
        @DisplayName("Classifies JSON as TEXT")
        void testJson() {
            String json = "{\"name\": \"Scan Pilot\", \"version\": \"1.0.0\", \"active\": true}";
            ClassificationResult result = classifierService.classify("package.json", json.getBytes(StandardCharsets.UTF_8));
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.mimeTypeHint()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("Classifies Markdown as TEXT")
        void testMarkdown() {
            String md = "# Scan Pilot\n\nSecurity health monitor for AI-generated code.";
            ClassificationResult result = classifierService.classify("README.md", md.getBytes(StandardCharsets.UTF_8));
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.mimeTypeHint()).isEqualTo("text/markdown");
        }

        @Test
        @DisplayName("Classifies HTML and XML as TEXT")
        void testHtmlAndXml() {
            String html = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Hello</h1></body></html>";
            ClassificationResult htmlResult = classifierService.classify("index.html", html.getBytes(StandardCharsets.UTF_8));
            assertThat(htmlResult.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(htmlResult.mimeTypeHint()).isEqualTo("text/html");

            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item id=\"1\"/></root>";
            ClassificationResult xmlResult = classifierService.classify("pom.xml", xml.getBytes(StandardCharsets.UTF_8));
            assertThat(xmlResult.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(xmlResult.mimeTypeHint()).isEqualTo("text/xml");
        }

        @Test
        @DisplayName("Classifies empty file (0 bytes) as TEXT")
        void testEmptyFile() {
            ClassificationResult result = classifierService.classify("empty.txt", new byte[0]);
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.confidence()).isEqualTo(1.0);
            assertThat(result.isBinaryDocument()).isFalse();
        }

        @Test
        @DisplayName("Classifies UTF-8 with BOM and multi-byte characters as TEXT")
        void testUtf8WithBomAndVietnameseCharacters() {
            byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] textBytes = "Kiểm tra phát hiện bí mật và bảo mật mã nguồn Scan Pilot 🚀".getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[bom.length + textBytes.length];
            System.arraycopy(bom, 0, combined, 0, bom.length);
            System.arraycopy(textBytes, 0, combined, bom.length, textBytes.length);

            ClassificationResult result = classifierService.classify("notes.txt", combined);
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.confidence()).isGreaterThan(0.9);
        }

        @Test
        @DisplayName("Classifies ISO-8859-1 encoded text as TEXT")
        void testIso8859Text() {
            byte[] isoBytes = "Café au lait, résumé et façade".getBytes(StandardCharsets.ISO_8859_1);
            ClassificationResult result = classifierService.classify("french.txt", isoBytes);
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
        }
    }

    @Nested
    @DisplayName("Binary Signatures and Document Detection")
    class BinarySignatureTests {

        @Test
        @DisplayName("Detects PDF document signature (%PDF)")
        void testPdfDocument() {
            byte[] pdfHeader = "%PDF-1.7\n%âãÏÓ\n1 0 obj\n<<...>>".getBytes(StandardCharsets.US_ASCII);
            ClassificationResult result = classifierService.classify("manual.pdf", pdfHeader);
            assertThat(result.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(result.isBinaryDocument()).isTrue();
            assertThat(result.mimeTypeHint()).isEqualTo("application/pdf");
        }

        @Test
        @DisplayName("Detects DOCX / XLSX / PPTX Office Open XML documents")
        void testOfficeOpenXmlDocuments() {
            // ZIP magic bytes PK\x03\x04
            byte[] zipHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};

            ClassificationResult docxResult = classifierService.classify("report.docx", zipHeader);
            assertThat(docxResult.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(docxResult.isBinaryDocument()).isTrue();

            ClassificationResult xlsxResult = classifierService.classify("data.xlsx", zipHeader);
            assertThat(xlsxResult.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(xlsxResult.isBinaryDocument()).isTrue();

            ClassificationResult pptxResult = classifierService.classify("slides.pptx", zipHeader);
            assertThat(pptxResult.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(pptxResult.isBinaryDocument()).isTrue();
        }

        @Test
        @DisplayName("Detects Office Open XML by inner zip structure even without extension")
        void testOfficeOpenXmlByContentSignature() {
            byte[] zipWithContentTypes = new byte[]{
                0x50, 0x4B, 0x03, 0x04, 0x00, 0x00,
                '[', 'C', 'o', 'n', 't', 'e', 'n', 't', '_', 'T', 'y', 'p', 'e', 's', ']', '.', 'x', 'm', 'l'
            };
            ClassificationResult result = classifierService.classify("document_without_ext", zipWithContentTypes);
            assertThat(result.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(result.isBinaryDocument()).isTrue();
        }

        @Test
        @DisplayName("Detects Legacy MS Office OLE compound documents (DOC/XLS/PPT)")
        void testLegacyOfficeDocument() {
            byte[] oleHeader = new byte[]{
                (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0x00, 0x00
            };
            ClassificationResult result = classifierService.classify("old_doc.doc", oleHeader);
            assertThat(result.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(result.isBinaryDocument()).isTrue();
            assertThat(result.mimeTypeHint()).isEqualTo("application/msword");
        }

        @Test
        @DisplayName("Detects ELF executable binary")
        void testElfBinary() {
            byte[] elfHeader = new byte[]{0x7F, 'E', 'L', 'F', 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            ClassificationResult result = classifierService.classify("app.elf", elfHeader);
            assertThat(result.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(result.isBinaryDocument()).isFalse();
            assertThat(result.mimeTypeHint()).isEqualTo("application/x-elf");
        }

        @Test
        @DisplayName("Detects Windows PE / DOS executable")
        void testPeBinary() {
            byte[] peHeader = new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
            ClassificationResult result = classifierService.classify("app.exe", peHeader);
            assertThat(result.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(result.isBinaryDocument()).isFalse();
            assertThat(result.mimeTypeHint()).isEqualTo("application/vnd.microsoft.portable-executable");
        }

        @Test
        @DisplayName("Detects PNG, JPEG, GIF, and WebP images")
        void testImageFormats() {
            byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
            ClassificationResult pngResult = classifierService.classify("logo.png", png);
            assertThat(pngResult.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(pngResult.mimeTypeHint()).isEqualTo("image/png");

            byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
            ClassificationResult jpegResult = classifierService.classify("photo.jpg", jpeg);
            assertThat(jpegResult.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(jpegResult.mimeTypeHint()).isEqualTo("image/jpeg");

            byte[] gif = "GIF89a\u0001\u0000\u0001\u0000".getBytes(StandardCharsets.US_ASCII);
            ClassificationResult gifResult = classifierService.classify("anim.gif", gif);
            assertThat(gifResult.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(gifResult.mimeTypeHint()).isEqualTo("image/gif");

            byte[] webp = new byte[]{'R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'};
            ClassificationResult webpResult = classifierService.classify("banner.webp", webp);
            assertThat(webpResult.classification()).isEqualTo(ContentClassification.BINARY);
            assertThat(webpResult.mimeTypeHint()).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("Detects ZIP, GZIP, 7-Zip, WebAssembly, and Java Class files")
        void testOtherBinarySignatures() {
            byte[] zip = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
            assertThat(classifierService.classify("archive.zip", zip).mimeTypeHint()).isEqualTo("application/zip");

            byte[] gzip = new byte[]{0x1F, (byte) 0x8B, 0x08, 0x00};
            assertThat(classifierService.classify("archive.tar.gz", gzip).mimeTypeHint()).isEqualTo("application/gzip");

            byte[] sevenZip = new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
            assertThat(classifierService.classify("archive.7z", sevenZip).mimeTypeHint()).isEqualTo("application/x-7z-compressed");

            byte[] wasm = new byte[]{0x00, 'a', 's', 'm', 0x01, 0x00, 0x00, 0x00};
            assertThat(classifierService.classify("module.wasm", wasm).mimeTypeHint()).isEqualTo("application/wasm");

            byte[] javaClass = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x00, 0x00, 0x00, 0x41};
            assertThat(classifierService.classify("App.class", javaClass).mimeTypeHint()).isEqualTo("application/java-vm");
        }
    }

    @Nested
    @DisplayName("Disguised and Ambiguous Files")
    class DisguisedFileTests {

        @Test
        @DisplayName("Binary payload disguised with .txt extension is detected as BINARY")
        void testBinaryDisguisedAsTxt() {
            // Null bytes inside .txt file
            byte[] binaryData = new byte[]{'H', 'e', 'l', 'l', 'o', 0x00, 0x01, 0x02, (byte) 0xFF};
            ClassificationResult result = classifierService.classify("secret.txt", binaryData);
            assertThat(result.classification()).isEqualTo(ContentClassification.BINARY);
        }

        @Test
        @DisplayName("ELF binary disguised as .txt is detected as BINARY")
        void testElfDisguisedAsTxt() {
            byte[] elfBytes = new byte[]{0x7F, 'E', 'L', 'F', 1, 1, 1, 0};
            ClassificationResult result = classifierService.classify("readme.txt", elfBytes);
            assertThat(result.classification()).isEqualTo(ContentClassification.BINARY);
        }

        @Test
        @DisplayName("Java source code named .bin or .dat is detected as TEXT")
        void testSourceCodeDisguisedAsBin() {
            String javaSource = "public class Hidden { private String secret = \"token123\"; }";
            ClassificationResult result = classifierService.classify("payload.bin", javaSource.getBytes(StandardCharsets.UTF_8));
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.isBinaryDocument()).isFalse();

            ClassificationResult datResult = classifierService.classify("config.dat", "{\"apiKey\": \"secret_xyz\"}".getBytes(StandardCharsets.UTF_8));
            assertThat(datResult.classification()).isEqualTo(ContentClassification.TEXT);
        }

        @Test
        @DisplayName("Ambiguous high-entropy stream with illegal controls returns BINARY or UNDETERMINED")
        void testAmbiguousStream() {
            // Non-UTF-8 stream with high concentration of control characters
            byte[] badBytes = new byte[]{(byte) 0x80, (byte) 0x81, 0x01, 0x02, 0x03, 0x04, (byte) 0x85, (byte) 0x86};
            ClassificationResult result = classifierService.classify("unknown.blob", badBytes);
            assertThat(result.classification()).isIn(ContentClassification.BINARY, ContentClassification.UNDETERMINED);
        }
    }

    @Nested
    @DisplayName("Stream and Path I/O Classification")
    class IoClassificationTests {

        @Test
        @DisplayName("Classifies from InputStream")
        void testInputStreamClassification() throws IOException {
            byte[] content = "SPRING_PROFILES_ACTIVE=prod\nDB_PASS=123456".getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
                ClassificationResult result = classifierService.classify(".env", in);
                assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
                assertThat(result.mimeTypeHint()).isEqualTo("text/plain");
            }
        }

        @Test
        @DisplayName("Classifies from file Path on disk")
        void testClassifiesFromDisk(@TempDir Path tempDir) throws IOException {
            Path testFile = tempDir.resolve("Config.java");
            Files.writeString(testFile, "public record Config(String apiKey) {}");

            ClassificationResult result = classifierService.classify(testFile);
            assertThat(result.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(result.mimeTypeHint()).isEqualTo("text/x-java-source");
        }

        @Test
        @DisplayName("Returns UNDETERMINED for non-existent file path")
        void testNonExistentPath() throws IOException {
            Path nonExistent = Path.of("non_existent_file_xyz_123.tmp");
            ClassificationResult result = classifierService.classify(nonExistent);
            assertThat(result.classification()).isEqualTo(ContentClassification.UNDETERMINED);
        }
    }
}
