package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Download {

    private final int id;
    private final String magnetLink;
    private final Path targetDirectory;
    private final BtClient client;
    private final CompletableFuture torrentFuture;
    private TorrentSessionState state;
    private boolean isCancelled = false;

    public Download(int id, String magnetLink, Path targetDirectory, BtClient client, CompletableFuture torrentFuture) {
        this.id = id;
        this.magnetLink = magnetLink;
        this.targetDirectory = targetDirectory;
        this.client = client;
        this.torrentFuture = torrentFuture;
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

    public String getMagnetLink() {
        return magnetLink;
    }

    public Path getTargetDirectory() {
        return targetDirectory;
    }

    public DownloadDto mapToDownloadDto() {
        double progress = getProgress();
        DownloadState downloadState = getDownloadState();
        return new DownloadDto(id, downloadState, progress, magnetLink);
    }

    private DownloadState getDownloadState() {
        if (isCancelled) return DownloadState.CANCELLED;
        if (isDone()) return DownloadState.FINISHED;
        return DownloadState.STARTED;
    }

    private double getProgress() {
        return ((double) state.getPiecesComplete()) / state.getPiecesTotal();
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
}
