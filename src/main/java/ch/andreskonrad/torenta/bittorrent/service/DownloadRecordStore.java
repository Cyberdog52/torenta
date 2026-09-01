package ch.andreskonrad.torenta.bittorrent.service;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecord;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecordState;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Object> writeLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastProgressPersistAt = new ConcurrentHashMap<>();

    @Autowired
    public DownloadRecordStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        writeInternal(root, record, true);
    }

    /** Progress-only persistence, throttled to once per second per download unless forced. */
    public void persistProgress(Path root, DownloadRecord record) {
        writeInternal(root, record, false);
    }

    private void writeInternal(Path root, DownloadRecord record, boolean force) {
        Object lock = writeLocks.computeIfAbsent(record.getId(), id -> new Object());
        synchronized (lock) {
            if (!force) {
                Long last = lastProgressPersistAt.get(record.getId());
                long now = System.currentTimeMillis();
                if (last != null && now - last < PROGRESS_PERSIST_INTERVAL_MS) {
                    return;
                }
            }
            lastProgressPersistAt.put(record.getId(), System.currentTimeMillis());
            try {
                Path dir = recordDirectory(root, record.getId());
                Files.createDirectories(dir);
                Path target = recordFile(root, record.getId());
                Path tmp = dir.resolve(RECORD_FILE_NAME + "." + UUID.randomUUID() + ".tmp");
                objectMapper.writeValue(tmp.toFile(), record);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new DownloadOperationException("Failed to persist download record " + record.getId(), e);
            }
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
            if (record.getVersion() != DownloadRecord.CURRENT_VERSION
                    || record.getId() == null || !record.getId().equals(id)
                    || record.getState() == null || record.getDownloadRequest() == null) {
                return new RecoveryEntry(id, null, "Record is missing required fields");
            }
            return new RecoveryEntry(id, reconcile(root, record), null);
        } catch (Exception e) {
            LOGGER.warn("Could not load download record {}: {}", id, e.toString());
            return new RecoveryEntry(id, null, e.getMessage());
        }
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
}
