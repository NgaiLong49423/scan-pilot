package com.scanpilot.scanner.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the bounded in-process scan executor queue capacity is exceeded.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class ScanCapacityExceededException extends RuntimeException {

    public ScanCapacityExceededException(String message) {
        super(message);
    }
}
