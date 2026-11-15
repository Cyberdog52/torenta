package ch.andreskonrad.torenta.bittorrent.service;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecord;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecordState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadResumeCheckpoint;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Owns the durable, filesystem-backed representation of every download at
 * {@code <download-root>/.torenta/downloads/<stable-id>/download.json} (metadata) and
 * {@code <download-root>/.torenta/downloads/<stable-id>/payload/} (staged torrent payload).
 * The in-memory {@link BitTorrentService} state is only a runtime projection of these records;
 * this store is the sole source of truth.
 */
@Component
public class DownloadRecordStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadRecordStore.class);

    static final String TORENTA_DIR_NAME = ".torenta";
    static final String DOWNLOADS_DIR_NAME = "downloads";
    static final String RECORD_FILE_NAME = "download.json";
    static final String PAYLOAD_DIR_NAME = "payload";
    static final long PROGRESS_PERSIST_INTERVAL_MS = 1000L;
    private static final int MAX_PROGRESS_WRITE_ATTEMPTS = 3;

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Object> writeLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<ProgressKey, Long> lastProgressPersistAt = new ConcurrentHashMap<>();
    private final ConcurrentMap<ProgressKey, PendingProgress> pendingProgress = new ConcurrentHashMap<>();
    private final ConcurrentMap<ProgressKey, ScheduledFuture<?>> scheduledProgress = new ConcurrentHashMap<>();
    private final ConcurrentMap<ProgressKey, Long> activeGenerations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService progressWriter;
    private final AtomicBoolean shuttingDown = new AtomicBoolean();

    @Autowired
    public DownloadRecordStore(ObjectMapper objectMapper) {
        this(objectMapper, Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "torenta-download-progress-writer");
            thread.setDaemon(true);
            return thread;
        }));
    }

    DownloadRecordStore(ObjectMapper objectMapper, ScheduledExecutorService progressWriter) {
        this.objectMapper = objectMapper;
        this.progressWriter = progressWriter;
    }

    public String stableId(String magnetLink) {
        return DownloadIdGenerator.generate(magnetLink);
    }

    public Path downloadsRoot(Path root) {
        return root.resolve(TORENTA_DIR_NAME).resolve(DOWNLOADS_DIR_NAME);
    }

    public Path recordDirectory(Path root, String id) {
        validateId(id);
        return downloadsRoot(root).resolve(id);
    }

    public Path recordFile(Path root, String id) {
        return recordDirectory(root, id).resolve(RECORD_FILE_NAME);
    }

    public Path payloadDirectory(Path root, String id) {
        return recordDirectory(root, id).resolve(PAYLOAD_DIR_NAME);
    }

    private void validateId(String id) {
        if (id == null || !id.matches("[a-f0-9]+")) {
            throw new IllegalArgumentException("Invalid stable download id: " + id);
        }
    }

    /**
     * Resolves a root-relative path stored on disk, rejecting traversal, absolute escape, and
     * symlink escape outside the download root. Only regular files/directories that genuinely
     * live under the root are ever returned.
     */
    public Path resolveRootRelative(Path root, String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative path must not be blank");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path candidate = normalizedRoot.resolve(relativePath).normalize();
        if (!candidate.equals(normalizedRoot) && !candidate.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Path escapes the download root: " + relativePath);
        }
        assertNoSymlinkEscape(normalizedRoot, candidate);
        return candidate;
    }

    /** Converts a trusted, already-confined absolute path into a portable root-relative string. */
    public String toRootRelative(Path root, Path absolutePath) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path candidate = absolutePath.toAbsolutePath().normalize();
        if (!candidate.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Path is not inside the download root: " + absolutePath);
        }
        Path relative = normalizedRoot.relativize(candidate);
        if (relative.getNameCount() == 0 || relative.toString().isBlank()) {
            return ".";
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < relative.getNameCount(); i++) {
            if (i > 0) {
                joined.append('/');
            }
            joined.append(relative.getName(i));
        }
        return joined.toString();
    }

    private void assertNoSymlinkEscape(Path normalizedRoot, Path candidate) throws IOException {
        Path realRoot = normalizedRoot.toRealPath();
        Path existingAncestor = candidate;
        while (existingAncestor != null && !Files.exists(existingAncestor)) {
            existingAncestor = existingAncestor.getParent();
        }
        if (existingAncestor == null) {
            return;
        }
        Path realExisting = existingAncestor.toRealPath();
        if (!realExisting.equals(realRoot) && !realExisting.startsWith(realRoot)) {
            throw new IllegalArgumentException("Path escapes the download root via a symlink: " + candidate);
        }
    }

    /** Forces an immediate durable write, used for every lifecycle/state transition. */
    public void writeRecord(Path root, DownloadRecord record) {
        ProgressKey key = ProgressKey.of(root, record.getId());
        synchronized (lockFor(record.getId())) {
            pendingProgress.remove(key);
            cancelScheduled(key);
            writeInternal(key, record);
            activeGenerations.put(key, record.getStartTimeInMs());
        }
    }

    /** Coalesces progress updates and writes them off the BitTorrent engine callback thread. */
    public void persistProgress(Path root, DownloadRecord record) {
        ProgressKey key = ProgressKey.of(root, record.getId());
        synchronized (lockFor(key.id())) {
            if (!isActiveGeneration(key, record)) {
                return;
            }
            if (shuttingDown.get()) {
                writeInternal(key, record);
                return;
            }
            pendingProgress.put(key, new PendingProgress(key, record));
            if (shuttingDown.get()) {
                PendingProgress pending = pendingProgress.remove(key);
                if (pending != null) {
                    writeInternal(pending.key(), pending.record());
                }
                return;
            }
            scheduleProgress(key);
        }
    }

    private void scheduleProgress(ProgressKey key) {
        synchronized (lockFor(key.id())) {
            if (shuttingDown.get() || scheduledProgress.containsKey(key)) {
                return;
            }
            long elapsed = System.currentTimeMillis() - lastProgressPersistAt.getOrDefault(key, 0L);
            long delay = Math.max(0, PROGRESS_PERSIST_INTERVAL_MS - elapsed);
            ScheduledFuture<?> future = progressWriter.schedule(
                    () -> drainProgress(key), delay, TimeUnit.MILLISECONDS);
            scheduledProgress.put(key, future);
        }
    }

    private void drainProgress(ProgressKey key) {
        boolean retry;
        synchronized (lockFor(key.id())) {
            scheduledProgress.remove(key);
            PendingProgress pending = pendingProgress.remove(key);
            if (pending != null && isActiveGeneration(key, pending.record())) {
                try {
                    writeInternal(pending.key(), pending.record());
                } catch (DownloadOperationException exception) {
                    if (pending.attempts() < MAX_PROGRESS_WRITE_ATTEMPTS) {
                        pendingProgress.putIfAbsent(key, pending.nextAttempt());
                        if (shuttingDown.get()) {
                            PendingProgress shutdownPending = pendingProgress.remove(key);
                            if (shutdownPending != null) {
                                try {
                                    writeInternal(shutdownPending.key(), shutdownPending.record());
                                } catch (DownloadOperationException shutdownException) {
                                    exception.addSuppressed(shutdownException);
                                }
                            }
                        }
                    }
                    LOGGER.error("Failed to persist progress for download {}", key.id(), exception);
                }
            }
            retry = !shuttingDown.get() && pendingProgress.containsKey(key);
        }
        if (retry) {
            scheduleProgress(key);
        }
    }

    private void writeInternal(ProgressKey key, DownloadRecord record) {
        lastProgressPersistAt.put(key, System.currentTimeMillis());
        Path tmp = null;
        try {
            Path dir = recordDirectory(key.root(), record.getId());
            Files.createDirectories(dir);
            Path target = recordFile(key.root(), record.getId());
            tmp = dir.resolve(RECORD_FILE_NAME + "." + UUID.randomUUID() + ".tmp");
            objectMapper.writeValue(tmp.toFile(), record);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new DownloadOperationException("Failed to persist download record " + record.getId(), e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException exception) {
                    LOGGER.warn("Failed to remove temporary download record {}", tmp, exception);
                }
            }
        }
    }

    private Object lockFor(String id) {
        return writeLocks.computeIfAbsent(id, ignored -> new Object());
    }

    private void cancelScheduled(ProgressKey key) {
        ScheduledFuture<?> future = scheduledProgress.remove(key);
        if (future != null) {
            future.cancel(false);
        }
    }

    DownloadRecord readRecord(Path root, String id) throws IOException {
        Path file = recordFile(root, id);
        if (!Files.isRegularFile(file)) {
            throw new IOException("No record file for download " + id);
        }
        return objectMapper.readValue(file.toFile(), DownloadRecord.class);
    }

    /** Recursively and permanently deletes a download's entire record directory (metadata + staging). */
    public void deleteRecordAndStaging(Path root, String id) throws IOException {
        discardProgress(ProgressKey.of(root, id));
        deleteRecursively(recordDirectory(root, id));
    }

    /** Deletes only the staged payload directory, keeping metadata intact. */
    public void deleteStagingOnly(Path root, String id) throws IOException {
        deleteRecursively(payloadDirectory(root, id));
    }

    /**
     * Deletes only the final files this record's manifest claims to own, preserving every other
     * file and all parent folders. Returns the root-relative paths that could not be deleted.
     */
    public List<String> deleteManifestFiles(Path root, List<String> manifest) {
        List<String> failures = new ArrayList<>();
        if (manifest == null) {
            return failures;
        }
        for (String relative : manifest) {
            try {
                Path resolved = resolveRootRelative(root, relative);
                Files.deleteIfExists(resolved);
            } catch (Exception e) {
                LOGGER.warn("Failed to delete manifest-owned file {}: {}", relative, e.toString());
                failures.add(relative);
            }
        }
        return failures;
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void discardProgress(ProgressKey key) {
        synchronized (lockFor(key.id())) {
            pendingProgress.remove(key);
            cancelScheduled(key);
            lastProgressPersistAt.remove(key);
            activeGenerations.remove(key);
        }
    }

    private boolean isActiveGeneration(ProgressKey key, DownloadRecord record) {
        return Long.valueOf(record.getStartTimeInMs()).equals(activeGenerations.get(key));
    }

    @PreDestroy
    public void shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }
        for (ProgressKey key : List.copyOf(scheduledProgress.keySet())) {
            synchronized (lockFor(key.id())) {
                cancelScheduled(key);
            }
        }
        progressWriter.shutdown();
        try {
            if (!progressWriter.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Progress writer did not stop within 5 seconds");
                progressWriter.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            progressWriter.shutdownNow();
        }
        for (ProgressKey key : List.copyOf(pendingProgress.keySet())) {
            synchronized (lockFor(key.id())) {
                PendingProgress pending = pendingProgress.remove(key);
                if (pending != null) {
                    try {
                        writeInternal(pending.key(), pending.record());
                    } catch (DownloadOperationException exception) {
                        LOGGER.error("Failed to flush progress for download {} during shutdown", key.id(), exception);
                    }
                }
            }
        }
    }

    /** Scans every persisted record under the root and applies startup recovery policy to each. */
    public List<RecoveryEntry> recoverAll(Path root) {
        Path downloadsRoot = downloadsRoot(root);
        List<RecoveryEntry> result = new ArrayList<>();
        if (!Files.isDirectory(downloadsRoot)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(downloadsRoot)) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                if (!dir.getFileName().toString().matches("[a-f0-9]+")) {
                    LOGGER.warn("Ignoring unexpected download record directory {}", dir);
                    continue;
                }
                result.add(recoverOne(root, dir.getFileName().toString()));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan download records under {}", downloadsRoot, e);
        }
        return result;
    }

    private RecoveryEntry recoverOne(Path root, String id) {
        try {
            DownloadRecord record = readRecord(root, id);
            if (record.getVersion() < 1 || record.getVersion() > DownloadRecord.CURRENT_VERSION
                    || record.getId() == null || !record.getId().equals(id)
                    || record.getState() == null || record.getDownloadRequest() == null) {
                return new RecoveryEntry(id, null, "Record is missing required fields");
            }
            boolean normalize = record.getVersion() < DownloadRecord.CURRENT_VERSION;
            if (normalize) {
                record = record.toBuilder()
                        .version(DownloadRecord.CURRENT_VERSION)
                        .resumeCheckpoint(null)
                        .build();
            } else if (!isValidResumeCheckpoint(record.getResumeCheckpoint())) {
                record = record.toBuilder().resumeCheckpoint(null).build();
                normalize = true;
            }
            DownloadRecord reconciled = reconcile(root, record);
            if (normalize) {
                writeRecord(root, reconciled);
            }
            return new RecoveryEntry(id, reconciled, null);
        } catch (Exception e) {
            LOGGER.warn("Could not load download record {}: {}", id, e.toString());
            return new RecoveryEntry(id, null, e.getMessage());
        }
    }

    private boolean isValidResumeCheckpoint(DownloadResumeCheckpoint checkpoint) {
        if (checkpoint == null) {
            return true;
        }
        if (checkpoint.torrentId() == null
                || !checkpoint.torrentId().matches("[a-fA-F0-9]{40}")
                || checkpoint.piecesTotal() <= 0
                || checkpoint.verifiedPieces() == null
                || checkpoint.files() == null
                || checkpoint.verifiedPieces().size() > checkpoint.piecesTotal()) {
            return false;
        }
        Set<Integer> seenPieces = new HashSet<>();
        for (Integer piece : checkpoint.verifiedPieces()) {
            if (piece == null || piece < 0 || piece >= checkpoint.piecesTotal() || !seenPieces.add(piece)) {
                return false;
            }
        }
        return checkpoint.files().stream().allMatch(file -> file != null
                && file.relativePath() != null
                && !file.relativePath().isBlank()
                && file.size() >= 0
                && file.fileKey() != null);
    }

    private DownloadRecord reconcile(Path root, DownloadRecord record) {
        DownloadRecord updated = record;
        if (record.getState() == DownloadRecordState.STARTED) {
            updated = record.toBuilder().state(DownloadRecordState.PAUSED).build();
        } else if (record.getState() == DownloadRecordState.FINISHED) {
            updated = verifyFinishedManifest(root, record);
        }
        if (updated != record) {
            writeRecord(root, updated);
        }
        return updated;
    }

    private DownloadRecord verifyFinishedManifest(Path root, DownloadRecord record) {
        List<String> manifest = record.getFinalPayloadManifest() == null ? List.of() : record.getFinalPayloadManifest();
        boolean allPresent = true;
        for (String relative : manifest) {
            try {
                Path resolved = resolveRootRelative(root, relative);
                if (!Files.isRegularFile(resolved)) {
                    allPresent = false;
                    break;
                }
            } catch (Exception e) {
                allPresent = false;
                break;
            }
        }
        if (!allPresent) {
            return record.toBuilder()
                    .state(DownloadRecordState.FAILED)
                    .failureKind(DownloadFailureKind.RESTARTABLE)
                    .errorMessage("One or more finished files are missing from the final destination.")
                    .build();
        }
        try {
            deleteStagingOnly(root, record.getId());
        } catch (IOException e) {
            LOGGER.warn("Failed to clean up stray staging for finished download {}: {}", record.getId(), e.toString());
        }
        return record;
    }

    /** Outcome of loading a single persisted record at startup. */
    public static final class RecoveryEntry {
        private final String id;
        private final DownloadRecord record;
        private final String invalidReason;

        RecoveryEntry(String id, DownloadRecord record, String invalidReason) {
            this.id = id;
            this.record = record;
            this.invalidReason = invalidReason;
        }

        public String getId() {
            return id;
        }

        public DownloadRecord getRecord() {
            return record;
        }

        public String getInvalidReason() {
            return invalidReason;
        }

        public boolean isValid() {
            return record != null;
        }
    }

    private record ProgressKey(Path root, String id) {
        private static ProgressKey of(Path root, String id) {
            return new ProgressKey(root.toAbsolutePath().normalize(), id);
        }
    }

    private record PendingProgress(ProgressKey key, DownloadRecord record, int attempts) {
        private PendingProgress(ProgressKey key, DownloadRecord record) {
            this(key, record, 1);
        }

        private PendingProgress nextAttempt() {
            return new PendingProgress(key, record, attempts + 1);
        }
    }
}
