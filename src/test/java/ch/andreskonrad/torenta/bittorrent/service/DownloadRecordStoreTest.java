package ch.andreskonrad.torenta.bittorrent.service;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecord;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecordState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadRecordStoreTest {

    private static final String MAGNET_LINK = "magnet:?xt=urn:btih:abcdef";

    @TempDir
    Path root;

    private DownloadRecordStore recordStore;
    private String id;

    @BeforeEach
    void setUp() {
        recordStore = new DownloadRecordStore(new ObjectMapper());
        id = DownloadIdGenerator.generate(MAGNET_LINK);
    }

    @Test
    void writeRecord_thenReadRecord_roundTripsEveryField() throws IOException {
        DownloadRecord record = baseRecordBuilder().build();

        recordStore.writeRecord(root, record);
        DownloadRecord read = recordStore.readRecord(root, id);

        assertEquals(record.getId(), read.getId());
        assertEquals(record.getState(), read.getState());
        assertEquals(record.getFinalTargetRelativePath(), read.getFinalTargetRelativePath());
        assertEquals(record.getDownloadRequest().getTorrentEntry().getMagnetLink(),
                read.getDownloadRequest().getTorrentEntry().getMagnetLink());
        assertEquals(record.getFinalPayloadManifest(), read.getFinalPayloadManifest());
    }

    @Test
    void writeRecord_isAtomicAndLeavesNoTemporaryFilesBehind() throws IOException {
        recordStore.writeRecord(root, baseRecordBuilder().build());

        try (var stream = Files.list(recordStore.recordDirectory(root, id))) {
            List<Path> files = stream.toList();
            assertEquals(1, files.size());
            assertEquals(recordStore.recordFile(root, id), files.get(0));
        }
    }

    @Test
    void persistProgress_throttlesRepeatedWritesWithinOneSecond() throws IOException, InterruptedException {
        recordStore.writeRecord(root, baseRecordBuilder().lastProgress(0).build());
        long firstModified = Files.getLastModifiedTime(recordStore.recordFile(root, id)).toMillis();

        recordStore.persistProgress(root, baseRecordBuilder().lastProgress(0.5).build());
        DownloadRecord afterThrottledWrite = recordStore.readRecord(root, id);

        assertEquals(0, afterThrottledWrite.getLastProgress());

        Thread.sleep(DownloadRecordStore.PROGRESS_PERSIST_INTERVAL_MS + 100);
        recordStore.persistProgress(root, baseRecordBuilder().lastProgress(0.75).build());
        DownloadRecord afterDelayedWrite = recordStore.readRecord(root, id);

        assertEquals(0.75, afterDelayedWrite.getLastProgress());
        assertTrue(firstModified <= Files.getLastModifiedTime(recordStore.recordFile(root, id)).toMillis());
    }

    @Test
    void resolveRootRelative_rejectsParentTraversal() {
        assertThrows(IllegalArgumentException.class, () -> recordStore.resolveRootRelative(root, "../escape.txt"));
    }

    @Test
    void recoverAll_ignoresUnexpectedNonIdDirectories() throws IOException {
        Files.createDirectories(recordStore.downloadsRoot(root).resolve("not-a-download"));

        assertTrue(recordStore.recoverAll(root).isEmpty());
    }

    @Test
    void resolveRootRelative_rejectsAbsolutePathEscape() {
        String absoluteOutsideRoot = root.getRoot().resolve("etc").resolve("passwd").toString();
        assertThrows(IllegalArgumentException.class, () -> recordStore.resolveRootRelative(root, absoluteOutsideRoot));
    }

    @Test
    void resolveRootRelative_rejectsSymlinkEscape() throws IOException {
        Path outsideTarget = Files.createTempDirectory("torenta-outside");
        try {
            Path linkParent = root.resolve("linked");
            try {
                Files.createSymbolicLink(linkParent, outsideTarget);
            } catch (FileSystemException exception) {
                assumeTrue(false, "Symbolic links are unavailable: " + exception.getReason());
            }

            assertThrows(IllegalArgumentException.class,
                    () -> recordStore.resolveRootRelative(root, "linked/file.txt"));
        } finally {
            deleteRecursivelyForTest(outsideTarget);
        }
    }

    @Test
    void resolveRootRelative_acceptsPathsGenuinelyInsideRoot() throws IOException {
        Path resolved = recordStore.resolveRootRelative(root, "Movies/Movie (2024)/movie.mkv");

        assertEquals(root.resolve("Movies").resolve("Movie (2024)").resolve("movie.mkv").toAbsolutePath().normalize(),
                resolved);
    }

    @Test
    void toRootRelative_convertsAbsolutePathToPortableRelativeString() {
        Path absolute = root.resolve("Movies").resolve("Movie (2024)").resolve("movie.mkv");

        String relative = recordStore.toRootRelative(root, absolute);

        assertEquals("Movies/Movie (2024)/movie.mkv", relative);
    }

    @Test
    void rootPath_roundTripsThroughPortableRelativeSentinel() throws IOException {
        String relative = recordStore.toRootRelative(root, root);

        assertEquals(".", relative);
        assertEquals(root.toAbsolutePath().normalize(), recordStore.resolveRootRelative(root, relative));
    }

    @Test
    void toRootRelative_rejectsPathOutsideRoot() {
        Path outside = root.resolve("..").resolve("outside.txt");
        assertThrows(IllegalArgumentException.class, () -> recordStore.toRootRelative(root, outside));
    }

    @Test
    void deleteManifestFiles_deletesEveryFileAndReportsNoFailuresWhenAllSucceed() throws IOException {
        Path fileA = root.resolve("a.mkv");
        Path fileB = root.resolve("b.mkv");
        Files.writeString(fileA, "a");
        Files.writeString(fileB, "b");

        List<String> failures = recordStore.deleteManifestFiles(root,
                List.of(recordStore.toRootRelative(root, fileA), recordStore.toRootRelative(root, fileB)));

        assertTrue(failures.isEmpty());
        assertFalse(Files.exists(fileA));
        assertFalse(Files.exists(fileB));
    }

    @Test
    void deleteManifestFiles_reportsEntriesThatEscapeTheRootAsFailuresWithoutThrowing() {
        List<String> failures = recordStore.deleteManifestFiles(root, List.of("../escape.txt"));

        assertEquals(List.of("../escape.txt"), failures);
    }

    @Test
    void deleteManifestFiles_toleratesAlreadyMissingFiles() {
        List<String> failures = recordStore.deleteManifestFiles(root, List.of("does-not-exist.mkv"));

        assertTrue(failures.isEmpty());
    }

    @Test
    void deleteRecordAndStaging_removesTheEntireRecordDirectory() throws IOException {
        Path payload = recordStore.payloadDirectory(root, id);
        Files.createDirectories(payload);
        Files.writeString(payload.resolve("part.bin"), "data");
        recordStore.writeRecord(root, baseRecordBuilder().build());

        recordStore.deleteRecordAndStaging(root, id);

        assertFalse(Files.exists(recordStore.recordDirectory(root, id)));
    }

    @Test
    void deleteStagingOnly_removesPayloadButKeepsMetadata() throws IOException {
        Path payload = recordStore.payloadDirectory(root, id);
        Files.createDirectories(payload);
        Files.writeString(payload.resolve("part.bin"), "data");
        recordStore.writeRecord(root, baseRecordBuilder().build());

        recordStore.deleteStagingOnly(root, id);

        assertFalse(Files.exists(payload));
        assertTrue(Files.exists(recordStore.recordFile(root, id)));
    }

    @Test
    void recoverAll_convertsStartedRecordToPaused() {
        recordStore.writeRecord(root, baseRecordBuilder().state(DownloadRecordState.STARTED).build());

        List<DownloadRecordStore.RecoveryEntry> entries = recordStore.recoverAll(root);

        assertEquals(1, entries.size());
        DownloadRecordStore.RecoveryEntry entry = entries.get(0);
        assertTrue(entry.isValid());
        assertEquals(DownloadRecordState.PAUSED, entry.getRecord().getState());
    }

    @Test
    void recoverAll_returnsInvalidEntryForCorruptJson() throws IOException {
        Path recordDir = recordStore.recordDirectory(root, id);
        Files.createDirectories(recordDir);
        Files.writeString(recordStore.recordFile(root, id), "not-valid-json");

        List<DownloadRecordStore.RecoveryEntry> entries = recordStore.recoverAll(root);

        assertEquals(1, entries.size());
        DownloadRecordStore.RecoveryEntry entry = entries.get(0);
        assertFalse(entry.isValid());
        assertNull(entry.getRecord());
        assertEquals(id, entry.getId());
    }

    @Test
    void recoverAll_finishedRecordWithAllManifestFilesPresent_staysFinishedAndCleansStrayStaging() throws IOException {
        Path finalFile = root.resolve("Movies").resolve("Movie").resolve("movie.mkv");
        Files.createDirectories(finalFile.getParent());
        Files.writeString(finalFile, "content");
        Path payload = recordStore.payloadDirectory(root, id);
        Files.createDirectories(payload);
        Files.writeString(payload.resolve("leftover.bin"), "stray");
        recordStore.writeRecord(root, baseRecordBuilder()
                .state(DownloadRecordState.FINISHED)
                .finalPayloadManifest(List.of(recordStore.toRootRelative(root, finalFile)))
                .build());

        List<DownloadRecordStore.RecoveryEntry> entries = recordStore.recoverAll(root);

        DownloadRecordStore.RecoveryEntry entry = entries.get(0);
        assertTrue(entry.isValid());
        assertEquals(DownloadRecordState.FINISHED, entry.getRecord().getState());
        assertFalse(Files.exists(payload));
    }

    @Test
    void recoverAll_finishedRecordWithMissingManifestFile_becomesRestartableFailure() {
        Path finalFile = root.resolve("Movies").resolve("Movie").resolve("missing.mkv");
        recordStore.writeRecord(root, baseRecordBuilder()
                .state(DownloadRecordState.FINISHED)
                .finalPayloadManifest(List.of(recordStore.toRootRelative(root, finalFile)))
                .build());

        List<DownloadRecordStore.RecoveryEntry> entries = recordStore.recoverAll(root);

        DownloadRecordStore.RecoveryEntry entry = entries.get(0);
        assertTrue(entry.isValid());
        assertEquals(DownloadRecordState.FAILED, entry.getRecord().getState());
        assertEquals(DownloadFailureKind.RESTARTABLE, entry.getRecord().getFailureKind());
    }

    @Test
    void recoverAll_onEmptyDownloadsRoot_returnsEmptyList() {
        List<DownloadRecordStore.RecoveryEntry> entries = recordStore.recoverAll(root);

        assertTrue(entries.isEmpty());
    }

    private DownloadRecord.DownloadRecordBuilder baseRecordBuilder() {
        DownloadRequest downloadRequest = new DownloadRequest(null, new TorrentEntry(
                "name", MAGNET_LINK, "link", "uploadedTime", "size", "uploader", 0, 0,
                "category", "subCategory", false, false), null, null);
        return DownloadRecord.builder()
                .version(DownloadRecord.CURRENT_VERSION)
                .id(id)
                .state(DownloadRecordState.STARTED)
                .downloadRequest(downloadRequest)
                .finalTargetRelativePath("Movies/Movie")
                .startTimeInMs(System.currentTimeMillis())
                .lastProgress(0)
                .lastTotalBytes(0)
                .finalPayloadManifest(List.of());
    }

    private void deleteRecursivelyForTest(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
