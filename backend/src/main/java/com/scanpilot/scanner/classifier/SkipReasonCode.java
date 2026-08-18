package com.scanpilot.scanner.classifier;

/**
 * Stable reason codes for skipping repository items from secret scanning.
 */
public enum SkipReasonCode {
    /**
     * Binary document format (PDF, DOCX, XLSX, PPTX, etc.) inventoried per FR-034.
     */
    UNSUPPORTED_BINARY_DOCUMENT,

    /**
     * General binary file format (PNG, JPG, EXE, DLL, ZIP, ELF, etc.).
     */
    UNSUPPORTED_BINARY_FILE,

    /**
     * Text file exceeding Continuous Monitoring limit of 10 MiB per FR-037.
     */
    MONITORING_FILE_SIZE_LIMIT_EXCEEDED,

    /**
     * Text file exceeding Release Assessment ceiling of 50 MiB per FR-037.
     */
    RELEASE_FILE_SIZE_CEILING_EXCEEDED,

    /**
     * Content whose classification is undetermined and skipped per policy FR-035.
     */
    UNDETERMINED_CONTENT_POLICY_SKIP,

    /**
     * Special repository object (e.g. submodules, broken symlinks).
     */
    UNSUPPORTED_SPECIAL_OBJECT
}
