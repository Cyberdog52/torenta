package ch.andreskonrad.torenta.bittorrent.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = {"id"})
public class DownloadDto {

    private final int id;
    private final DownloadState state;
    private final double progress;
    private final DownloadRequest downloadRequest;
    private final long startTimeInMs;
    private final long connectedPeers;
    private final long totalBytes;
    private final double downloadSpeedInBytesPerSecond;
}
