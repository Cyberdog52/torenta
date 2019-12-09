package ch.andreskonrad.torenta.bittorrent.dto;

import java.util.Objects;

public class DownloadDto {

    private final int id;
    private final DownloadState state;
    private final double progress;
    private final DownloadRequest downloadRequest;
    private final long startTimeInMs;

    public DownloadDto(int id, DownloadState state, double progress, DownloadRequest downloadRequest, long startTimeInMs) {
        this.id = id;
        this.state = state;
        this.progress = progress;
        this.downloadRequest = downloadRequest;
        this.startTimeInMs = startTimeInMs;
    }


    public double getProgress() {
        return progress;
    }

    public int getId() {
        return id;
    }

    public DownloadState getState() {
        return state;
    }

    public DownloadRequest getDownloadRequest() {
        return downloadRequest;
    }

    public long getStartTimeInMs() {
        return startTimeInMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadDto that = (DownloadDto) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
