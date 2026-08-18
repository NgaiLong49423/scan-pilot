package com.scanpilot.github.service;

import com.scanpilot.github.config.GitHubAppConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class GitHubAppAuthService {

    public static final String GITHUB_INSTALLATION_TOKEN_URL = "https://api.github.com/app/installations/%d/access_tokens";

    private final GitHubAppConfigProperties properties;
    private final RestClient restClient;

    public GitHubAppAuthService(GitHubAppConfigProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public boolean isConfigured() {
        return properties.getAppId() != null && !properties.getAppId().isBlank()
                && properties.getAppPrivateKey() != null && !properties.getAppPrivateKey().isBlank();
    }

    /**
     * Generates a GitHub App JWT using RSA Private Key (PKCS8/PKCS1 PEM) with 10-min TTL.
     */
    public String generateAppJwt() {
        if (!isConfigured()) {
            throw new IllegalStateException("GitHub App is not configured (missing appId or appPrivateKey)");
        }

        try {
            PrivateKey privateKey = parsePrivateKey(properties.getAppPrivateKey());

            long now = Instant.now().getEpochSecond();
            long iat = now - 60; // 60s in the past for clock drift
            long exp = iat + (10 * 60); // 10 minutes from iat

            String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
            String payloadJson = String.format("{\"iat\":%d,\"exp\":%d,\"iss\":\"%s\"}", iat, exp, properties.getAppId().trim());

            String headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = headerEncoded + "." + payloadEncoded;

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();

            String signatureEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            return dataToSign + "." + signatureEncoded;
        } catch (Exception e) {
            log.error("Failed to generate GitHub App JWT", e);
            throw new IllegalStateException("Failed to generate GitHub App JWT: " + e.getMessage(), e);
        }
    }

    /**
     * Generates on-demand ephemeral installation access token.
     */
    public String createInstallationAccessToken(Long installationId) {
        if (installationId == null) {
            throw new IllegalArgumentException("Installation ID cannot be null");
        }

        String jwt = generateAppJwt();
        String uri = String.format(GITHUB_INSTALLATION_TOKEN_URL, installationId);

        Map<?, ?> response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("token")) {
            throw new IllegalStateException("Failed to obtain installation access token from GitHub");
        }

        return (String) response.get("token");
    }

    public PrivateKey parsePrivateKey(String keyPem) throws Exception {
        if (keyPem == null || keyPem.isBlank()) {
            throw new IllegalArgumentException("Private key PEM cannot be null or blank");
        }

        String cleanPem = keyPem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decodedKey = Base64.getDecoder().decode(cleanPem);

        try {
            // Try standard PKCS#8
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            // Try wrapping PKCS#1 to PKCS#8
            byte[] pkcs8Bytes = wrapPkcs1ToPkcs8(decodedKey);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }
    }

    private byte[] wrapPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        // AlgorithmIdentifier for RSA encryption (1.2.840.113549.1.1.1)
        byte[] rsaOid = new byte[]{
                0x30, 0x0d,
                0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        // Version 0
        byte[] version = new byte[]{0x02, 0x01, 0x00};

        // OCTET STRING for pkcs1Bytes
        byte[] octetString = encodeDer(0x04, pkcs1Bytes);

        int totalLength = version.length + rsaOid.length + octetString.length;
        byte[] sequenceHeader = encodeLength(0x30, totalLength);

        byte[] result = new byte[sequenceHeader.length + totalLength];
        int pos = 0;
        System.arraycopy(sequenceHeader, 0, result, pos, sequenceHeader.length);
        pos += sequenceHeader.length;
        System.arraycopy(version, 0, result, pos, version.length);
        pos += version.length;
        System.arraycopy(rsaOid, 0, result, pos, rsaOid.length);
        pos += rsaOid.length;
        System.arraycopy(octetString, 0, result, pos, octetString.length);

        return result;
    }

    private byte[] encodeDer(int tag, byte[] content) {
        byte[] lengthHeader = encodeLength(tag, content.length);
        byte[] result = new byte[lengthHeader.length + content.length];
        System.arraycopy(lengthHeader, 0, result, 0, lengthHeader.length);
        System.arraycopy(content, 0, result, lengthHeader.length, content.length);
        return result;
    }

    private byte[] encodeLength(int tag, int length) {
        if (length < 128) {
            return new byte[]{(byte) tag, (byte) length};
        } else if (length < 256) {
            return new byte[]{(byte) tag, (byte) 0x81, (byte) length};
        } else if (length < 65536) {
            return new byte[]{(byte) tag, (byte) 0x82, (byte) (length >> 8), (byte) (length & 0xFF)};
        } else {
            return new byte[]{(byte) tag, (byte) 0x83, (byte) (length >> 16), (byte) ((length >> 8) & 0xFF), (byte) (length & 0xFF)};
        }
    }
}
