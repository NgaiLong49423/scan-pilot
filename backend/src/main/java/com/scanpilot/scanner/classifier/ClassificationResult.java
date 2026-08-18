package com.scanpilot.scanner.classifier;

import lombok.Builder;

/**
 * Result of content classification for a single repository file.
 *
 * @param classification   The determined classification (TEXT, BINARY, UNDETERMINED).
 * @param mimeTypeHint     Detected or inferred MIME type hint.
 * @param isBinaryDocument True if identified as a binary document (PDF, Office OOXML, OLE doc).
 * @param confidence       Confidence score between 0.0 and 1.0.
 * @param detail           Human-readable diagnostic detail.
 */
@Builder
public record ClassificationResult(
    ContentClassification classification,
    String mimeTypeHint,
    boolean isBinaryDocument,
    double confidence,
    String detail
) {
    public static ClassificationResult text(String mimeTypeHint, double confidence, String detail) {
        return new ClassificationResult(ContentClassification.TEXT, mimeTypeHint, false, confidence, detail);
    }

    public static ClassificationResult binary(String mimeTypeHint, boolean isBinaryDocument, double confidence, String detail) {
        return new ClassificationResult(ContentClassification.BINARY, mimeTypeHint, isBinaryDocument, confidence, detail);
    }

    public static ClassificationResult undetermined(String detail) {
        return new ClassificationResult(ContentClassification.UNDETERMINED, "application/octet-stream", false, 0.0, detail);
    }
}
