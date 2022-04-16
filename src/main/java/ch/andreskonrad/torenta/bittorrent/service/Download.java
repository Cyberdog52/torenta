package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Download {

    private final int id;
    private final DownloadRequest downloadRequest;
    private final Path targetDirectory;
    private final BtClient client;
    private final CompletableFuture torrentFuture;
    private TorrentSessionState state;
    private boolean isCancelled = false;
    private final long startTimeInMs;

    public Download(int id, DownloadRequest downloadRequest, Path targetDirectory, BtClient client, CompletableFuture torrentFuture) {
        this.id = id;
        this.downloadRequest = downloadRequest;
        this.targetDirectory = targetDirectory;
        this.client = client;
        this.torrentFuture = torrentFuture;
        this.startTimeInMs = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public BtClient getClient() {
        return client;
    }


    public void cancel() {
        //client.stop(); TODO: is this needed
        isCancelled = true;
        torrentFuture.cancel(true);
    }

    public boolean isDone() {
        return torrentFuture.isDone();
    }

    public TorrentSessionState getState() {
        return state;
    }

    public void setState(TorrentSessionState state) {
        this.state = state;
    }

    public DownloadRequest getDownloadRequest() {
        return downloadRequest;
    }

    public Path getTargetDirectory() {
        return targetDirectory;
    }

    public DownloadDto mapToDownloadDto() {
        double progress = getProgress();
        DownloadState downloadState = getDownloadState();
        return new DownloadDto(id, downloadState, progress, downloadRequest, this.startTimeInMs, this.getConnectedPeers(), this.getTotalBytes(), this.getDownloadSpeedInBytesPerSecond());
    }

    private long getTotalBytes() {
        return this.state.getChunksSizeInBytes() * this.state.getPiecesTotal();
    }

    private DownloadState getDownloadState() {
        if (isCancelled) return DownloadState.CANCELLED;
        if (isDone()) return DownloadState.FINISHED;
        return DownloadState.STARTED;
    }

    private double getProgress() {
        return ((double) state.getPiecesComplete()) / state.getPiecesTotal();
    }

    private long getConnectedPeers() {
        return this.state.getConnectedPeers() != null ? this.state.getConnectedPeers().size() : 0;
    }

    private double getDownloadSpeedInBytesPerSecond() {
        long amountOfChunksSavedInLastMinute = this.state.getSaveTimesOfChunks().stream()
                .filter(localDateTime -> localDateTime.isAfter(LocalDateTime.now().minusMinutes(1)))
                .count();
        long chunkSize = this.state.getChunksSizeInBytes();
        return amountOfChunksSavedInLastMinute * chunkSize / 60.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Download download = (Download) o;
        return id == download.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public long getStartTimeInMs() {
        return startTimeInMs;
    }
}
