package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadActionCapabilities;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecordState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Runtime projection of a single download's durable {@code DownloadRecord}. Combines the
 * (possibly absent) live BitTorrent engine handle with the last-known persisted lifecycle state
 * so the UI always has something to render, whether the download is actively running, paused,
 * recovered from a previous process, finished, or failed.
 */
public class Download {

    static final long PEER_DISCOVERY_TIMEOUT_IN_MS = 5 * 60 * 1000L;

    private final String id;
    private final Path stagingDirectory;
    private final Path finalTargetDirectory;
    private final long startTimeInMs;
    private volatile long sessionStartTimeInMs;

    private volatile DownloadRequest downloadRequest;
    private volatile BtClient client;
    private volatile CompletableFuture<?> torrentFuture;
    private volatile TorrentSessionState state;
    private volatile boolean peersEverConnected = false;
    private volatile boolean paused = false;

    private volatile DownloadRecordState recordState = DownloadRecordState.STARTED;
    private volatile DownloadFailureKind failureKind;
    private volatile String errorMessage;
    private volatile List<String> finalPayloadManifest = List.of();
    private volatile double recoveredProgress = 0;
    private volatile long recoveredTotalBytes = 0;

    public Download(String id, DownloadRequest downloadRequest, Path stagingDirectory, Path finalTargetDirectory,
                     BtClient client, CompletableFuture<?> torrentFuture) {
        this(id, downloadRequest, stagingDirectory, finalTargetDirectory, client, torrentFuture, System.currentTimeMillis());
    }

    Download(String id, DownloadRequest downloadRequest, Path stagingDirectory, Path finalTargetDirectory,
             BtClient client, CompletableFuture<?> torrentFuture, long startTimeInMs) {
        this.id = id;
        this.downloadRequest = downloadRequest;
        this.stagingDirectory = stagingDirectory;
        this.finalTargetDirectory = finalTargetDirectory;
        this.client = client;
        this.torrentFuture = Objects.requireNonNull(torrentFuture);
        this.startTimeInMs = startTimeInMs;
        this.sessionStartTimeInMs = startTimeInMs;
    }

    /** Builds a runtime projection for a download recovered from disk with no live engine yet. */
    static Download recovered(String id, DownloadRequest downloadRequest, Path stagingDirectory, Path finalTargetDirectory,
                               long startTimeInMs, DownloadRecordState recordState, DownloadFailureKind failureKind,
                               String errorMessage, double recoveredProgress, long recoveredTotalBytes,
                               List<String> finalPayloadManifest) {
        Download download = new Download(id, downloadRequest, stagingDirectory, finalTargetDirectory, null,
                CompletableFuture.completedFuture(null), startTimeInMs);
        download.recordState = recordState;
        download.failureKind = failureKind;
        download.errorMessage = errorMessage;
        download.recoveredProgress = recoveredProgress;
        download.recoveredTotalBytes = recoveredTotalBytes;
        download.finalPayloadManifest = finalPayloadManifest == null ? List.of() : finalPayloadManifest;
        download.paused = recordState == DownloadRecordState.PAUSED;
        return download;
    }

    public String getId() {
        return id;
    }

    public BtClient getClient() {
        return client;
    }

    public Path getStagingDirectory() {
        return stagingDirectory;
    }

    public Path getFinalTargetDirectory() {
        return finalTargetDirectory;
    }

    public DownloadRequest getDownloadRequest() {
        return downloadRequest;
    }

    public long getStartTimeInMs() {
        return startTimeInMs;
    }

    public TorrentSessionState getState() {
        return state;
    }

    public void setState(TorrentSessionState state) {
        this.state = state;
        if (state != null && state.getConnectedPeers() != null && !state.getConnectedPeers().isEmpty()) {
            this.peersEverConnected = true;
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DownloadFailureKind getFailureKind() {
        return failureKind;
    }

    public DownloadRecordState getRecordState() {
        return recordState;
    }

    public List<String> getFinalPayloadManifest() {
        return finalPayloadManifest;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isDone() {
        return torrentFuture.isDone();
    }

    public boolean isComplete() {
        TorrentSessionState currentState = state;
        return currentState != null
                && currentState.getPiecesTotal() > 0
                && currentState.getPiecesComplete() >= currentState.getPiecesTotal();
    }

    /** Marks this download as pausing before its client is stopped, so the resulting future
     * completion/cancellation is never mistaken for a failure. */
    public void beginPause() {
        this.paused = true;
    }

    public void cancelPause() {
        this.paused = false;
    }

    public void markPaused() {
        this.paused = true;
        this.recordState = DownloadRecordState.PAUSED;
    }

    public void fail(String errorMessage, DownloadFailureKind failureKind) {
        this.recordState = DownloadRecordState.FAILED;
        this.failureKind = failureKind;
        this.errorMessage = errorMessage;
    }

    public void markFinished(List<String> manifest) {
        this.recordState = DownloadRecordState.FINISHED;
        this.finalPayloadManifest = manifest;
        this.errorMessage = null;
        this.failureKind = null;
    }

    /** Rebinds this download to a freshly created engine client/future, e.g. for a restart. */
    public void restart(BtClient newClient, CompletableFuture<?> newFuture) {
        this.client = newClient;
        this.paused = false;
        this.recordState = DownloadRecordState.STARTED;
        this.failureKind = null;
        this.errorMessage = null;
        this.peersEverConnected = false;
        this.sessionStartTimeInMs = System.currentTimeMillis();
        this.state = null;
        setTorrentFuture(newFuture);
    }

    void setTorrentFuture(CompletableFuture<?> torrentFuture) {
        this.torrentFuture = Objects.requireNonNull(torrentFuture);
    }

    public double getRecoveredProgress() {
        return recoveredProgress;
    }

    public long getRecoveredTotalBytes() {
        return recoveredTotalBytes;
    }

    public void setRecoveredMetrics(double progress, long totalBytes) {
        this.recoveredProgress = progress;
        this.recoveredTotalBytes = totalBytes;
    }

    public DownloadDto mapToDownloadDto() {
        double progress = getProgress();
        long totalBytes = getTotalBytes();
        long connectedPeers = getConnectedPeers();
        double speed = getDownloadSpeedInBytesPerSecond();
        String effectiveErrorMessage = getEffectiveErrorMessage();
        DownloadState downloadState = mapToDisplayState(effectiveErrorMessage);
        DownloadFailureKind effectiveFailureKind = downloadState == DownloadState.FAILED
                ? (failureKind != null ? failureKind : DownloadFailureKind.RESTARTABLE)
                : null;
        DownloadActionCapabilities capabilities = computeCapabilities();
        return new DownloadDto(id, downloadState, effectiveFailureKind, computeDisplayTitle(), progress,
                downloadRequest, startTimeInMs, connectedPeers, totalBytes, speed, effectiveErrorMessage, capabilities);
    }

    /** Derives a human-readable title from the download request; null only for an undecodable record. */
    private String computeDisplayTitle() {
        if (downloadRequest == null) {
            return null;
        }
        if (downloadRequest.getMovieDetail() != null) {
            return downloadRequest.getMovieDetail().getTitle();
        }
        if (downloadRequest.getSeriesDetail() != null) {
            String seriesName = downloadRequest.getSeriesDetail().getName();
            if (downloadRequest.getTmdbEpisode() != null) {
                return "%s S%02dE%02d".formatted(seriesName,
                        downloadRequest.getTmdbEpisode().getSeason_number(),
                        downloadRequest.getTmdbEpisode().getEpisode_number());
            }
            return seriesName;
        }
        if (downloadRequest.getTorrentEntry() != null) {
            return downloadRequest.getTorrentEntry().getName();
        }
        return null;
    }

    private boolean isActivelyRunning() {
        return client != null && recordState == DownloadRecordState.STARTED && !paused && !isDone();
    }

    private DownloadActionCapabilities computeCapabilities() {
        boolean running = isActivelyRunning();
        boolean canRestart = recordState == DownloadRecordState.PAUSED
                || (recordState == DownloadRecordState.FAILED && failureKind == DownloadFailureKind.RESTARTABLE);
        boolean canStopAndDelete = running || recordState == DownloadRecordState.PAUSED
                || recordState == DownloadRecordState.FAILED;
        boolean canRemove = recordState == DownloadRecordState.FINISHED;
        return new DownloadActionCapabilities(running, canRestart, canStopAndDelete, canRemove);
    }

    private DownloadState mapToDisplayState(String effectiveErrorMessage) {
        if (recordState == DownloadRecordState.FAILED || effectiveErrorMessage != null) {
            return DownloadState.FAILED;
        }
        if (recordState == DownloadRecordState.PAUSED) {
            return DownloadState.PAUSED;
        }
        if (recordState == DownloadRecordState.FINISHED) {
            return DownloadState.FINISHED;
        }
        return DownloadState.STARTED;
    }

    private String getEffectiveErrorMessage() {
        if (recordState == DownloadRecordState.FAILED) {
            return errorMessage;
        }
        if (isActivelyRunning() && !peersEverConnected
                && System.currentTimeMillis() - sessionStartTimeInMs > PEER_DISCOVERY_TIMEOUT_IN_MS) {
            return "No peers found within " + (PEER_DISCOVERY_TIMEOUT_IN_MS / 60_000)
                    + " minutes. The trackers of this torrent are unreachable or have no seeders.";
        }
        return null;
    }

    private double getProgress() {
        if (recordState == DownloadRecordState.FINISHED) {
            return 1.0;
        }
        TorrentSessionState currentState = state;
        if (currentState != null && currentState.getPiecesTotal() > 0) {
            return ((double) currentState.getPiecesComplete()) / currentState.getPiecesTotal();
        }
        return recoveredProgress;
    }

    private long getTotalBytes() {
        TorrentSessionState currentState = state;
        if (currentState != null) {
            return currentState.getChunksSizeInBytes() * currentState.getPiecesTotal();
        }
        return recoveredTotalBytes;
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
        return Objects.equals(id, download.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
