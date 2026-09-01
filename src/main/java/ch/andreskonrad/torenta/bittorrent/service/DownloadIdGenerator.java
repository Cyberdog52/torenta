package ch.andreskonrad.torenta.bittorrent.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Derives a collision-resistant, filesystem-safe stable ID for a download from its magnet link.
 * Using a SHA-256 digest (rather than {@link Object#hashCode()}) keeps the ID stable across JVM
 * restarts and versions, and safe to use as a directory name.
 */
final class DownloadIdGenerator {

    private DownloadIdGenerator() {
    }

    static String generate(String magnetLink) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(magnetLink.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every standard JVM implementation.
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
