package com.scanpilot.github.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BoundedStreamReader {

    private BoundedStreamReader() {
    }

    public static class PayloadTooLargeException extends RuntimeException {
        public PayloadTooLargeException(String message) {
            super(message);
        }
    }

    public static byte[] readBoundedStream(InputStream is, int maxBytes) throws IOException {
        if (is == null) {
            return new byte[0];
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int totalBytesRead = 0;
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            totalBytesRead += bytesRead;
            if (totalBytesRead > maxBytes) {
                throw new PayloadTooLargeException("Payload exceeds maximum permitted size of " + maxBytes + " bytes");
            }
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
}
