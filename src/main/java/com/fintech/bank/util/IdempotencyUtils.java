package com.fintech.bank.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Utility for computing deterministic cryptographic request fingerprints.
 */
public final class IdempotencyUtils {

    private IdempotencyUtils() {
    }

    /**
     * Generates a deterministic SHA-256 fingerprint from the HTTP method, request path,
     * authenticated user ID, and normalized request payload.
     */
    public static String computeRequestFingerprint(String method, String path, UUID actorId, Object payload, ObjectMapper objectMapper) {
        try {
            String payloadJson = "";
            if (payload != null) {
                if (payload instanceof String str) {
                    payloadJson = str.trim();
                } else {
                    payloadJson = objectMapper.writeValueAsString(payload);
                }
            }

            String actorStr = actorId != null ? actorId.toString() : "ANONYMOUS";
            String rawString = (method != null ? method.toUpperCase() : "POST")
                    + "|" + (path != null ? path.trim() : "")
                    + "|" + actorStr
                    + "|" + payloadJson;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate request fingerprint payload", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
