package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecord;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecordState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BitTorrentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BitTorrentService.class);
    private static final int SESSION_STATE_UPDATE_INTERVAL = 100;

    private final DirectoryService directoryService;
    private final BitTorrentClientFactory clientFactory;
    private final DownloadRecordStore recordStore;
    private final ConcurrentMap<String, Download> downloads = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> operationLocks = new ConcurrentHashMap<>();

    private volatile Path loadedRoot;
    private volatile boolean rootLoaded = false;

    public BitTorrentService(DirectoryService directoryService, DownloadRecordStore recordStore) {
        this(directoryService, new DefaultBitTorrentClientFactory(), recordStore);
    }

    @Autowired
    public BitTorrentService(DirectoryService directoryService, BitTorrentClientFactory clientFactory,
                              DownloadRecordStore recordStore) {
        this.directoryService = directoryService;
        this.clientFactory = clientFactory;
        this.recordStore = recordStore;
    }

    /**
     * Reconciles the in-memory runtime projection with the currently configured download root.
     * If the root has changed since the last call, only records under the new root are loaded;
     * any downloads tracked for a previous root are dropped from memory without touching their
     * files.
     */
    private synchronized void ensureLoadedForCurrentRoot() {
        Path currentRoot = directoryService.getRootDirectoryPath();
        if (rootLoaded && Objects.equals(loadedRoot, currentRoot)) {
            return;
        }
        if (rootLoaded && loadedRoot != null) {
            pauseDownloadsForRootChange(loadedRoot);
        }
        downloads.clear();
        loadedRoot = currentRoot;
        rootLoaded = true;
        if (currentRoot == null) {
            return;
        }
        List<DownloadRecordStore.RecoveryEntry> entries = recordStore.recoverAll(currentRoot);
        if (entries == null) {
            return;
        }
        for (DownloadRecordStore.RecoveryEntry entry : entries) {
            downloads.put(entry.getId(), toRuntimeDownload(currentRoot, entry));
        }
    }

    private void pauseDownloadsForRootChange(Path previousRoot) {
        for (Download download : downloads.values()) {
            if (download.getRecordState() != DownloadRecordState.STARTED || download.isPaused()) {
                continue;
            }
            synchronized (lockFor(download.getId())) {
                download.beginPause();
                try {
                    stopClient(download);
                } catch (DownloadOperationException exception) {
                    download.cancelPause();
                    throw exception;
                }
                download.markPaused();
                persistLifecycle(previousRoot, download);
            }
        }
    }

    private Download toRuntimeDownload(Path root, DownloadRecordStore.RecoveryEntry entry) {
        Path stagingDirectory = recordStore.payloadDirectory(root, entry.getId());
        if (!entry.isValid()) {
            return Download.recovered(entry.getId(), null, stagingDirectory, null, 0L,
                    DownloadRecordState.FAILED, DownloadFailureKind.CLEANUP_ONLY,
                    "Invalid download record: " + entry.getInvalidReason(), 0, 0, List.of());
        }
        DownloadRecord record = entry.getRecord();
        Path finalTargetDirectory;
        try {
            finalTargetDirectory = record.getFinalTargetRelativePath() == null
                    ? null
                    : recordStore.resolveRootRelative(root, record.getFinalTargetRelativePath());
        } catch (Exception e) {
            return Download.recovered(record.getId(), record.getDownloadRequest(), stagingDirectory, null,
                    record.getStartTimeInMs(), DownloadRecordState.FAILED, DownloadFailureKind.CLEANUP_ONLY,
                    "Invalid final target path: " + e.getMessage(), record.getLastProgress(),
                    record.getLastTotalBytes(), record.getFinalPayloadManifest());
        }
        return Download.recovered(record.getId(), record.getDownloadRequest(), stagingDirectory, finalTargetDirectory,
                record.getStartTimeInMs(), record.getState(), record.getFailureKind(), record.getErrorMessage(),
                record.getLastProgress(), record.getLastTotalBytes(), record.getFinalPayloadManifest());
    }

    public synchronized void startDownload(DownloadRequest downloadRequest, Path finalTargetDirectory) throws IllegalStateException {
        ensureLoadedForCurrentRoot();
        Path root = loadedRoot;
        String magnetLink = downloadRequest.getTorrentEntry().getMagnetLink();
        String id = DownloadIdGenerator.generate(magnetLink);
        if (downloads.get(id) != null) {
            throw new IllegalStateException("Already downloading a torrent with magnet link: " + magnetLink);
        }

        Path stagingDirectory = recordStore.payloadDirectory(root, id);
        try {
            Files.createDirectories(stagingDirectory);
        } catch (IOException e) {
            throw new DownloadOperationException("Failed to create staging directory for download " + id, e);
        }

        long startTimeInMs = System.currentTimeMillis();
        String finalTargetRelativePath = recordStore.toRootRelative(root, finalTargetDirectory);
        Path validatedFinalTarget;
        try {
            validatedFinalTarget = recordStore.resolveRootRelative(root, finalTargetRelativePath);
        } catch (IOException | IllegalArgumentException exception) {
            throw new DownloadOperationException("Final target directory is not safely contained in the download root",
                    exception);
        }
        DownloadRecord record = DownloadRecord.builder()
                .version(DownloadRecord.CURRENT_VERSION)
                .id(id)
                .state(DownloadRecordState.STARTED)
                .downloadRequest(downloadRequest)
                .finalTargetRelativePath(finalTargetRelativePath)
                .startTimeInMs(startTimeInMs)
                .lastProgress(0)
                .lastTotalBytes(0)
                .finalPayloadManifest(List.of())
                .build();
        recordStore.writeRecord(root, record);

        BtClient client;
        try {
            client = clientFactory.create(getConfig(), stagingDirectory, magnetLink.replace(" ", "%20"));
        } catch (RuntimeException | Error exception) {
            Download failedDownload = Download.recovered(id, downloadRequest, stagingDirectory,
                    validatedFinalTarget, startTimeInMs, DownloadRecordState.FAILED,
                    DownloadFailureKind.RESTARTABLE, rootCauseMessage(exception), 0, 0, List.of());
            downloads.put(id, failedDownload);
            persistLifecycle(root, failedDownload);
            throw exception;
        }
        Download download = new Download(id, downloadRequest, stagingDirectory, validatedFinalTarget, client,
                new CompletableFuture<>(), startTimeInMs);
        downloads.put(id, download);
        try {
            CompletableFuture<?> torrentFuture = client.startAsync(
                    torrentSessionState -> processSessionState(torrentSessionState, id),
                    SESSION_STATE_UPDATE_INTERVAL);
            torrentFuture.whenComplete((result, throwable) -> handleCompletion(id, throwable));
            download.setTorrentFuture(torrentFuture);
        } catch (RuntimeException | Error exception) {
            download.fail(rootCauseMessage(exception), DownloadFailureKind.RESTARTABLE);
            persistLifecycle(root, download);
            throw exception;
        }
    }

    public void startDownloadToPreferredFolder(DownloadRequest downloadRequest) {
        Path preferredDownloadFolder = this.directoryService.getRootDirectoryPath();
        if (downloadRequest.getSeriesDetail() != null && downloadRequest.getTmdbEpisode() != null) {
            preferredDownloadFolder = this.directoryService.createDirectoryToSaveSeries(downloadRequest.getSeriesDetail().getName(), downloadRequest.getTmdbEpisode().getSeason_number());
        }
        if (downloadRequest.getMovieDetail() != null) {
            preferredDownloadFolder = this.directoryService.createDirectoryToSaveMovie(downloadRequest.getMovieDetail().getTitle(), getReleaseYear(downloadRequest.getMovieDetail()));
        }
        startDownload(downloadRequest, preferredDownloadFolder);
    }

    /** Pauses a currently running download by stopping its engine client without failing it. */
    public void pause(String id) {
        ensureLoadedForCurrentRoot();
        Path root = loadedRoot;
        Download download = requireDownload(id);
        synchronized (lockFor(id)) {
            if (download.getRecordState() != DownloadRecordState.STARTED || download.isPaused()) {
                throw new InvalidDownloadStateException("Download " + id + " is not running and cannot be paused.");
            }
            download.beginPause();
            try {
                stopClient(download);
            } catch (DownloadOperationException exception) {
                download.cancelPause();
                throw exception;
            }
            download.markPaused();
            persistLifecycle(root, download);
        }
    }

    /** Restarts a paused, recovered, or restartable failed download with a fresh engine client. */
    public void restart(String id) {
        ensureLoadedForCurrentRoot();
        Path root = loadedRoot;
        Download download = requireDownload(id);
        synchronized (lockFor(id)) {
            boolean restartable = download.getRecordState() == DownloadRecordState.PAUSED
                    || (download.getRecordState() == DownloadRecordState.FAILED
                    && download.getFailureKind() == DownloadFailureKind.RESTARTABLE);
            if (!restartable || download.getDownloadRequest() == null) {
                throw new InvalidDownloadStateException("Download " + id + " cannot be restarted from its current state.");
            }
            try {
                Files.createDirectories(download.getStagingDirectory());
            } catch (IOException e) {
                throw new DownloadOperationException("Failed to prepare staging directory for download " + id, e);
            }
            String magnetLink = download.getDownloadRequest().getTorrentEntry().getMagnetLink();
            BtClient client = clientFactory.create(getConfig(), download.getStagingDirectory(), magnetLink.replace(" ", "%20"));
            download.restart(client, new CompletableFuture<>());
            persistLifecycle(root, download);
            try {
                CompletableFuture<?> torrentFuture = client.startAsync(
                        torrentSessionState -> processSessionState(torrentSessionState, id),
                        SESSION_STATE_UPDATE_INTERVAL);
                torrentFuture.whenComplete((result, throwable) -> handleCompletion(id, throwable));
                download.setTorrentFuture(torrentFuture);
            } catch (RuntimeException | Error exception) {
                download.fail(rootCauseMessage(exception), DownloadFailureKind.RESTARTABLE);
                persistLifecycle(root, download);
                throw exception;
            }
        }
    }

    /**
     * Stops the engine (if running) and permanently deletes only this download's isolated staging
     * data plus any final files it durably owns, then removes its metadata. If deletion cannot be
     * completed, the record is retained as a cleanup-only failure with the remaining ownership
     * information so retrying is safe.
     */
    public void stopAndDelete(String id) {
        ensureLoadedForCurrentRoot();
        Path root = loadedRoot;
        Download download = requireDownload(id);
        synchronized (lockFor(id)) {
            if (download.getRecordState() == DownloadRecordState.FINISHED) {
                throw new InvalidDownloadStateException("Use remove for a finished download " + id + ".");
            }
            if (download.getDownloadRequest() == null) {
                deleteEverythingOrThrow(root, id);
                downloads.remove(id, download);
                return;
            }
            if (download.getRecordState() == DownloadRecordState.STARTED && !download.isPaused()) {
                download.beginPause();
                try {
                    stopClient(download);
                } catch (DownloadOperationException exception) {
                    download.cancelPause();
                    throw exception;
                }
            }

            List<String> remainingManifest = new ArrayList<>();
            List<String> manifest = download.getFinalPayloadManifest();
            if (manifest != null && !manifest.isEmpty()) {
                remainingManifest.addAll(recordStore.deleteManifestFiles(root, manifest));
            }
            boolean stagingDeleted = true;
            try {
                recordStore.deleteStagingOnly(root, id);
            } catch (IOException e) {
                stagingDeleted = false;
                LOGGER.warn("Failed to delete staging directory for download {}: {}", id, e.toString());
            }

            if (!remainingManifest.isEmpty() || !stagingDeleted) {
                download.fail("Cleanup could not remove all owned files; retry to finish deleting.",
                        DownloadFailureKind.CLEANUP_ONLY);
                DownloadRecord record = toRecord(root, download).toBuilder()
                        .finalPayloadManifest(remainingManifest)
                        .build();
                recordStore.writeRecord(root, record);
                throw new DownloadOperationException("Cleanup for download " + id + " is incomplete; retry stop and delete.");
            }

            deleteEverythingOrThrow(root, id);
            downloads.remove(id, download);
        }
    }

    /** Removes a finished tile's metadata/staging remnants only; final media files are preserved. */
    public void remove(String id) {
        ensureLoadedForCurrentRoot();
        Path root = loadedRoot;
        Download download = requireDownload(id);
        synchronized (lockFor(id)) {
            if (download.getRecordState() != DownloadRecordState.FINISHED) {
                throw new InvalidDownloadStateException("Only a finished download can be removed: " + id);
            }
            deleteEverythingOrThrow(root, id);
            downloads.remove(id, download);
        }
    }

    private void deleteEverythingOrThrow(Path root, String id) {
        try {
            recordStore.deleteRecordAndStaging(root, id);
        } catch (IOException e) {
            throw new DownloadOperationException("Failed to delete metadata for download " + id, e);
        }
    }

    /** Persists any still-running downloads as paused and stops their engine clients. */
    @PreDestroy
    public void shutdown() {
        Path root = loadedRoot;
        if (root == null) {
            return;
        }
        for (Download download : downloads.values()) {
            if (download.getRecordState() == DownloadRecordState.STARTED && !download.isPaused()) {
                synchronized (lockFor(download.getId())) {
                    download.beginPause();
                    try {
                        stopClient(download);
                    } catch (DownloadOperationException exception) {
                        download.cancelPause();
                        LOGGER.warn("Error stopping engine client for download {} during shutdown",
                                download.getId(), exception);
                        continue;
                    }
                    download.markPaused();
                    persistLifecycle(root, download);
                }
            }
        }
    }

    private void stopClient(Download download) {
        BtClient client = download.getClient();
        if (client == null) {
            return;
        }
        try {
            client.stop();
        } catch (Exception e) {
            throw new DownloadOperationException("Failed to stop engine client for download " + download.getId(), e);
        }
    }

    private void handleCompletion(String id, Throwable throwable) {
        Download download = downloads.get(id);
        if (download == null) {
            return;
        }
        synchronized (lockFor(id)) {
            if (download.isPaused()) {
                return;
            }
            if (throwable instanceof CancellationException) {
                return;
            }
            Path root = loadedRoot;
            if (throwable != null) {
                LOGGER.error("Torrent download {} failed", id, throwable);
                download.fail(rootCauseMessage(throwable), DownloadFailureKind.RESTARTABLE);
                persistLifecycle(root, download);
            } else if (download.isComplete()) {
                finalizeDownload(root, download);
            } else {
                LOGGER.error("Torrent download {} stopped before completion. The BitTorrent processing chain was "
                        + "terminated; enable DEBUG logging for the 'bt' package to see the failing stage.", id);
                download.fail("Torrent processing was terminated before the download completed.", DownloadFailureKind.RESTARTABLE);
                persistLifecycle(root, download);
            }
        }
    }

    /**
     * Copies the isolated staging payload into the validated final destination with overwrite
     * enabled, persists the finished manifest, and only then removes staging. This is safe to
     * retry: a crash mid-copy leaves the record as {@code STARTED}, which startup recovery turns
     * into {@code PAUSED} so a restart re-verifies and re-copies.
     */
    private void finalizeDownload(Path root, Download download) {
        Path staging = download.getStagingDirectory();
        Path finalTarget = download.getFinalTargetDirectory();
        try {
            Files.createDirectories(finalTarget);
            List<String> manifest = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(staging)) {
                List<Path> files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
                for (Path file : files) {
                    Path relative = staging.relativize(file);
                    Path destination = finalTarget.resolve(relative);
                    if (destination.getParent() != null) {
                        Files.createDirectories(destination.getParent());
                    }
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                    manifest.add(recordStore.toRootRelative(root, destination));
                }
            }
            download.markFinished(manifest);
            persistLifecycle(root, download);
            recordStore.deleteStagingOnly(root, download.getId());
        } catch (Exception e) {
            LOGGER.error("Finalization failed for download {}", download.getId(), e);
            download.fail("Finalizing the download failed: " + rootCauseMessage(e), DownloadFailureKind.RESTARTABLE);
            persistLifecycle(root, download);
        }
    }

    private void persistLifecycle(Path root, Download download) {
        if (root == null) {
            return;
        }
        recordStore.writeRecord(root, toRecord(root, download));
    }

    private DownloadRecord toRecord(Path root, Download download) {
        DownloadDto dto = download.mapToDownloadDto();
        String finalTargetRelative = download.getFinalTargetDirectory() == null
                ? null
                : recordStore.toRootRelative(root, download.getFinalTargetDirectory());
        return DownloadRecord.builder()
                .version(DownloadRecord.CURRENT_VERSION)
                .id(download.getId())
                .state(download.getRecordState())
                .failureKind(download.getFailureKind())
                .downloadRequest(download.getDownloadRequest())
                .finalTargetRelativePath(finalTargetRelative)
                .startTimeInMs(download.getStartTimeInMs())
                .lastProgress(dto.getProgress())
                .lastTotalBytes(dto.getTotalBytes())
                .finalPayloadManifest(download.getFinalPayloadManifest())
                .errorMessage(download.getErrorMessage())
                .build();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getName() : cause.getClass().getSimpleName() + ": " + message;
    }

    private Integer getReleaseYear(TmdbMovieDetailDto movieDetailDto) {
        String releaseDate = movieDetailDto.getRelease_date();
        if (releaseDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(releaseDate).getYear();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private void processSessionState(TorrentSessionState state, String id) {
        Download download = downloads.get(id);
        if (download == null) {
            LOGGER.warn("Received session state for unknown download id {}", id);
            return;
        }
        synchronized (lockFor(id)) {
            download.setState(state);
            Path root = loadedRoot;
            if (root != null) {
                recordStore.persistProgress(root, toRecord(root, download));
            }
        }
    }

    private Object lockFor(String id) {
        return operationLocks.computeIfAbsent(id, key -> new Object());
    }

    private Download requireDownload(String id) {
        Download download = downloads.get(id);
        if (download == null) {
            throw new DownloadNotFoundException(id);
        }
        return download;
    }

    public Download getDownload(String id) {
        ensureLoadedForCurrentRoot();
        return downloads.get(id);
    }

    private Config getConfig() {
        Config config = new Config() {
            @Override
            public int getNumOfHashingThreads() {
                return Runtime.getRuntime().availableProcessors() * 2;
            }
        };
        RoutableAddressResolver.resolve().ifPresentOrElse(
                address -> {
                    config.setAcceptorAddress(address);
                    LOGGER.info("BitTorrent engine bound to local address {}", address.getHostAddress());
                },
                () -> LOGGER.warn("Could not determine a locally routable address; falling back to {}. "
                                + "If downloads find no peers, this interface likely has no internet route.",
                        config.getAcceptorAddress()));
        return config;
    }

    public Set<DownloadDto> getAllDownloadDtos() {
        ensureLoadedForCurrentRoot();
        return downloads.values().stream()
                .map(Download::mapToDownloadDto)
                .collect(Collectors.toSet());
    }
}
