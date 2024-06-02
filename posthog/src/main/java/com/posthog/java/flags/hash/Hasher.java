package com.posthog.java.flags.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
    private static final long LONG_SCALE = 0xfffffffffffffffL;

    public static double hash(String key, String distinctId, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update((key + "." + distinctId + salt).getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            String hexString = bytesToHex(hash).substring(0, 15);
            long value = Long.parseLong(hexString, 16);
            return (double) value / LONG_SCALE;
        } catch (NoSuchAlgorithmException | NumberFormatException e) {
            throw new RuntimeException("Hashing error: " + e.getMessage(), e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
