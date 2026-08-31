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
    private volatile CompletableFuture<?> torrentFuture;
    private volatile TorrentSessionState state;
    private volatile boolean isCancelled = false;
    private final long startTimeInMs;

    public Download(int id, DownloadRequest downloadRequest, Path targetDirectory, BtClient client, CompletableFuture<?> torrentFuture) {
        this.id = id;
        this.downloadRequest = downloadRequest;
        this.targetDirectory = targetDirectory;
        this.client = client;
        this.torrentFuture = Objects.requireNonNull(torrentFuture);
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

    void setTorrentFuture(CompletableFuture<?> torrentFuture) {
        this.torrentFuture = Objects.requireNonNull(torrentFuture);
        if (isCancelled) {
            torrentFuture.cancel(true);
        }
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
        TorrentSessionState currentState = state;
        return currentState == null
                ? 0
                : currentState.getChunksSizeInBytes() * currentState.getPiecesTotal();
    }

    private DownloadState getDownloadState() {
        if (isCancelled) return DownloadState.CANCELLED;
        if (isDone()) return DownloadState.FINISHED;
        return DownloadState.STARTED;
    }

    private double getProgress() {
        TorrentSessionState currentState = state;
        if (currentState == null || currentState.getPiecesTotal() == 0) {
            return 0;
        }
        return ((double) currentState.getPiecesComplete()) / currentState.getPiecesTotal();
    }

    private long getConnectedPeers() {
        TorrentSessionState currentState = state;
        return currentState != null && currentState.getConnectedPeers() != null
                ? currentState.getConnectedPeers().size()
                : 0;
    }

    private double getDownloadSpeedInBytesPerSecond() {
        TorrentSessionState currentState = state;
        if (currentState == null) {
            return 0;
        }
        long amountOfChunksSavedInLastMinute = currentState.getSaveTimesOfChunks().stream()
                .filter(localDateTime -> localDateTime.isAfter(LocalDateTime.now().minusMinutes(1)))
                .count();
        long chunkSize = currentState.getChunksSizeInBytes();
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
