package ch.andreskonrad.torenta.bittorrent.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = {"id"})
public class DownloadDto {

    private final String id;
    private final DownloadState state;
    /** Non-null only when {@link #state} is {@link DownloadState#FAILED}. */
    private final DownloadFailureKind failureKind;
    /** Human-readable title derived from the download request; null only for an invalid/undecodable record. */
    private final String displayTitle;
    private final double progress;
    /** Null only for an invalid record whose stored request could not be decoded. */
    private final DownloadRequest downloadRequest;
    private final long startTimeInMs;
    private final long activeDownloadTimeInMs;
    private final long connectedPeers;
    private final long totalBytes;
    private final double downloadSpeedInBytesPerSecond;
    private final String errorMessage;
    private final DownloadActionCapabilities capabilities;
}
