package com.scanpilot.scanner.classifier;

/**
 * High-level classification of repository content.
 */
public enum ContentClassification {
    /**
     * Textual content eligible for source/secret analysis.
     */
    TEXT,

    /**
     * Binary content (executables, archives, images, binary documents, etc.).
     */
    BINARY,

    /**
     * Content whose classification could not be determined conclusively.
     */
    UNDETERMINED
}
