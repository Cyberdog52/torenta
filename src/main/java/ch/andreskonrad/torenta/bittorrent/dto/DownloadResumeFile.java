package ch.andreskonrad.torenta.bittorrent.dto;

public record DownloadResumeFile(
        String relativePath,
        long size,
        long lastModifiedNanos,
        String fileKey) {
}
