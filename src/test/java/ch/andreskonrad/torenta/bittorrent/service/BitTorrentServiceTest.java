package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecord;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecordState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitTorrentServiceTest {

    private static final String MAGNET_LINK = "magnet:?xt=urn:btih:abc def";

    @TempDir
    Path root;

    @TempDir
    Path secondRoot;

    private DirectoryService directoryService;
    private BitTorrentClientFactory clientFactory;
    private DownloadRecordStore recordStore;
    private BtClient client;
    private CompletableFuture<Void> future;
    private BitTorrentService service;

    @BeforeEach
    void setUp() {
        directoryService = mock(DirectoryService.class);
        clientFactory = mock(BitTorrentClientFactory.class);
        recordStore = new DownloadRecordStore(new ObjectMapper());
        client = mock(BtClient.class);
        future = new CompletableFuture<>();
        lenient().when(directoryService.getRootDirectoryPath()).thenReturn(root);
        lenient().when(clientFactory.create(any(Config.class), any(Path.class), any(String.class))).thenReturn(client);
        lenient().doReturn(future).when(client).startAsync(any(), anyLong());
        service = new BitTorrentService(directoryService, clientFactory, recordStore);
    }

    @Test
    void startDownloadToPreferredFolder_usesPreferredRootWithoutMediaDetails() {
        DownloadRequest request = request(MAGNET_LINK);

        service.startDownloadToPreferredFolder(request);

        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        verify(clientFactory).create(any(Config.class), eq(recordStore.payloadDirectory(root, id)), eq("magnet:?xt=urn:btih:abc%20def"));
        assertEquals(root, service.getDownload(id).getFinalTargetDirectory());
    }

    @Test
    void startDownloadToPreferredFolder_usesSeriesSeasonDirectory() {
        Path seasonDirectory = root.resolve("Series").resolve("Show").resolve("S03");
        TmdbSeriesDetailDto series = mock(TmdbSeriesDetailDto.class);
        TmdbEpisodeDto episode = mock(TmdbEpisodeDto.class);
        when(series.getName()).thenReturn("Show");
        when(episode.getSeason_number()).thenReturn(3);
        when(directoryService.createDirectoryToSaveSeries("Show", 3)).thenReturn(seasonDirectory);
        DownloadRequest request = request(MAGNET_LINK);
        when(request.getSeriesDetail()).thenReturn(series);
        when(request.getTmdbEpisode()).thenReturn(episode);

        service.startDownloadToPreferredFolder(request);

        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        verify(directoryService).createDirectoryToSaveSeries("Show", 3);
        verify(clientFactory).create(any(Config.class), eq(recordStore.payloadDirectory(root, id)), eq("magnet:?xt=urn:btih:abc%20def"));
        assertEquals(seasonDirectory, service.getDownload(id).getFinalTargetDirectory());
    }

    @Test
    void startDownloadToPreferredFolder_usesMovieDirectoryWithValidReleaseYear() {
        Path movieDirectory = root.resolve("Movies").resolve("Movie (2024)");
        TmdbMovieDetailDto movie = movie("Movie", "2024-06-15");
        when(directoryService.createDirectoryToSaveMovie("Movie", 2024)).thenReturn(movieDirectory);
        DownloadRequest request = request(MAGNET_LINK);
        when(request.getMovieDetail()).thenReturn(movie);

        service.startDownloadToPreferredFolder(request);

        verify(directoryService).createDirectoryToSaveMovie("Movie", 2024);
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        verify(clientFactory).create(any(Config.class), eq(recordStore.payloadDirectory(root, id)), eq("magnet:?xt=urn:btih:abc%20def"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-a-date", "2024-13-40", "2024"})
    void startDownloadToPreferredFolder_usesMovieDirectoryWithoutYearForMissingOrMalformedReleaseDate(String releaseDate) {
        Path movieDirectory = root.resolve("Movies").resolve("Movie");
        TmdbMovieDetailDto movie = movie("Movie", releaseDate);
        when(directoryService.createDirectoryToSaveMovie("Movie", null)).thenReturn(movieDirectory);
        DownloadRequest request = request(MAGNET_LINK);
        when(request.getMovieDetail()).thenReturn(movie);

        service.startDownloadToPreferredFolder(request);

        verify(directoryService).createDirectoryToSaveMovie("Movie", null);
    }

    @Test
    void startDownload_encodesSpacesAndUsesExpectedClientConfiguration() {
        Path target = root.resolve("target");
        DownloadRequest request = request(MAGNET_LINK);
        ArgumentCaptor<Config> configCaptor = ArgumentCaptor.forClass(Config.class);

        service.startDownload(request, target);

        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        verify(clientFactory).create(configCaptor.capture(), eq(recordStore.payloadDirectory(root, id)), eq("magnet:?xt=urn:btih:abc%20def"));
        verify(client).startAsync(any(), eq(100L));
        assertEquals(Runtime.getRuntime().availableProcessors() * 2,
                configCaptor.getValue().getNumOfHashingThreads());
    }

    @Test
    void startDownload_rejectsDuplicateMagnetLink() {
        DownloadRequest request = request(MAGNET_LINK);
        service.startDownload(request, root.resolve("first"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.startDownload(request, root.resolve("second")));

        assertTrue(exception.getMessage().contains(MAGNET_LINK));
    }

    @Test
    void sessionCallbacks_updateTheirMatchingDownloads() {
        String secondMagnet = "magnet:?xt=urn:btih:second";
        CompletableFuture<Void> secondFuture = new CompletableFuture<>();
        doReturn(future, secondFuture).when(client).startAsync(any(), anyLong());
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();

        service.startDownload(request(MAGNET_LINK), root.resolve("first"));
        service.startDownload(request(secondMagnet), root.resolve("second"));
        verify(client, times(2)).startAsync(callbackCaptor.capture(), eq(100L));
        TorrentSessionState firstState = state(1, 4);
        TorrentSessionState secondState = state(3, 4);
        String firstId = DownloadIdGenerator.generate(MAGNET_LINK);
        String secondId = DownloadIdGenerator.generate(secondMagnet);

        callbackCaptor.getAllValues().get(0).accept(firstState);

        assertSame(firstState, service.getDownload(firstId).getState());
        assertNull(service.getDownload(secondId).getState());

        callbackCaptor.getAllValues().get(1).accept(secondState);

        assertSame(secondState, service.getDownload(secondId).getState());
    }

    @Test
    void startDownload_registersDownloadBeforeSynchronousSessionCallback() {
        TorrentSessionState initialState = state(1, 4);
        doAnswer(invocation -> {
            Consumer<TorrentSessionState> callback = invocation.getArgument(0);
            callback.accept(initialState);
            return future;
        }).when(client).startAsync(any(), anyLong());

        service.startDownload(request(MAGNET_LINK), root.resolve("target"));

        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        Download download = service.getDownload(id);
        assertNotNull(download);
        assertSame(initialState, download.getState());
    }

    @Test
    void getAllDownloadDtos_beforeFirstSessionCallbackReturnsZeroedStartedDownload() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));

        DownloadDto download = service.getAllDownloadDtos().iterator().next();

        assertEquals(DownloadState.STARTED, download.getState());
        assertEquals(0, download.getProgress());
        assertEquals(0, download.getConnectedPeers());
        assertEquals(0, download.getTotalBytes());
        assertEquals(0, download.getDownloadSpeedInBytesPerSecond());
        assertTrue(download.getCapabilities().isCanPause());
    }

    @Test
    void startDownload_startFailureKeepsRestartableFailedDownload() {
        doAnswer(invocation -> {
            throw new IllegalStateException("start failed");
        }).when(client).startAsync(any(), anyLong());

        assertThrows(
                IllegalStateException.class,
                () -> service.startDownload(request(MAGNET_LINK), root.resolve("target")));

        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        DownloadDto failed = service.getDownload(id).mapToDownloadDto();
        assertEquals(DownloadState.FAILED, failed.getState());
        assertEquals(DownloadFailureKind.RESTARTABLE, failed.getFailureKind());
        assertTrue(failed.getCapabilities().isCanRestart());
    }

    @Test
    void getAllDownloadDtos_collectsEveryDownload() {
        String secondMagnet = "magnet:?xt=urn:btih:second";
        doReturn(future, new CompletableFuture<>()).when(client).startAsync(any(), anyLong());
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();
        DownloadRequest firstRequest = request(MAGNET_LINK);
        DownloadRequest secondRequest = request(secondMagnet);
        service.startDownload(firstRequest, root.resolve("first"));
        service.startDownload(secondRequest, root.resolve("second"));
        verify(client, times(2)).startAsync(callbackCaptor.capture(), eq(100L));
        callbackCaptor.getAllValues().get(0).accept(state(1, 4));
        callbackCaptor.getAllValues().get(1).accept(state(2, 4));

        Set<DownloadDto> result = service.getAllDownloadDtos();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getDownloadRequest() == firstRequest));
        assertTrue(result.stream().anyMatch(dto -> dto.getDownloadRequest() == secondRequest));
    }

    @Test
    void downloads_areOwnedByEachConfiguredRootIndependently() {
        DirectoryService secondDirectoryService = mock(DirectoryService.class);
        when(secondDirectoryService.getRootDirectoryPath()).thenReturn(secondRoot);
        BitTorrentService secondService = new BitTorrentService(secondDirectoryService, clientFactory, recordStore);
        doReturn(future, new CompletableFuture<>()).when(client).startAsync(any(), anyLong());

        service.startDownload(request(MAGNET_LINK), root.resolve("first"));
        secondService.startDownload(request(MAGNET_LINK), secondRoot.resolve("second"));

        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        assertNotNull(service.getDownload(id));
        assertNotNull(secondService.getDownload(id));
    }

    @Test
    void ensureLoadedForCurrentRoot_rootChange_dropsOldInMemoryDownloadsWithoutTouchingFiles() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        assertNotNull(service.getDownload(id));

        when(directoryService.getRootDirectoryPath()).thenReturn(secondRoot);

        assertNull(service.getDownload(id));
        assertTrue(Files.exists(recordStore.recordFile(root, id)));
    }

    @Test
    void downloadFailure_isSurfacedAsFailedStateWithRootCauseMessage() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));

        future.completeExceptionally(new IllegalStateException("wrapper", new IllegalArgumentException("no metadata")));

        DownloadDto dto = service.getAllDownloadDtos().iterator().next();
        assertEquals(DownloadState.FAILED, dto.getState());
        assertEquals(DownloadFailureKind.RESTARTABLE, dto.getFailureKind());
        assertEquals("IllegalArgumentException: no metadata", dto.getErrorMessage());
    }

    @Test
    void downloadTerminatedBeforeCompletion_isSurfacedAsFailedState() {
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        verify(client).startAsync(callbackCaptor.capture(), eq(100L));
        callbackCaptor.getValue().accept(state(1, 4));

        future.complete(null);

        DownloadDto dto = service.getAllDownloadDtos().iterator().next();
        assertEquals(DownloadState.FAILED, dto.getState());
        assertTrue(dto.getErrorMessage().contains("terminated"));
    }

    @Test
    void completedDownload_finalizesPayloadOverwritingDestinationAndPersistsFinishedManifest() throws IOException {
        Path finalTarget = root.resolve("Movies").resolve("Movie (2024)");
        Files.createDirectories(finalTarget);
        Files.writeString(finalTarget.resolve("movie.mkv"), "stale-content");
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();
        service.startDownload(request(MAGNET_LINK), finalTarget);
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        Path staging = recordStore.payloadDirectory(root, id);
        Files.createDirectories(staging.resolve("sub"));
        Files.writeString(staging.resolve("movie.mkv"), "fresh-content");
        Files.writeString(staging.resolve("sub").resolve("extra.txt"), "extra");
        verify(client).startAsync(callbackCaptor.capture(), eq(100L));
        callbackCaptor.getValue().accept(state(4, 4));

        future.complete(null);

        DownloadDto dto = service.getAllDownloadDtos().iterator().next();
        assertEquals(DownloadState.FINISHED, dto.getState());
        assertEquals(1.0, dto.getProgress());
        assertEquals("fresh-content", Files.readString(finalTarget.resolve("movie.mkv")));
        assertEquals("extra", Files.readString(finalTarget.resolve("sub").resolve("extra.txt")));
        assertFalse(Files.exists(staging));
        DownloadRecord record = recordStore.readRecord(root, id);
        assertEquals(DownloadRecordState.FINISHED, record.getState());
        assertTrue(record.getFinalPayloadManifest().contains("Movies/Movie (2024)/movie.mkv"));
    }

    @Test
    void pause_stopsClientAndPersistsPausedRecord() throws IOException {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);

        service.pause(id);

        DownloadDto dto = service.getAllDownloadDtos().iterator().next();
        assertEquals(DownloadState.PAUSED, dto.getState());
        verify(client).stop();
        assertEquals(DownloadRecordState.PAUSED, recordStore.readRecord(root, id).getState());
    }

    @Test
    void pause_alreadyPaused_throwsInvalidState() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        service.pause(id);

        assertThrows(InvalidDownloadStateException.class, () -> service.pause(id));
    }

    @Test
    void pause_stopFailureKeepsDownloadRunningAndSurfacesError() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        doThrow(new IllegalStateException("stop failed")).when(client).stop();

        assertThrows(DownloadOperationException.class, () -> service.pause(id));

        DownloadDto dto = service.getDownload(id).mapToDownloadDto();
        assertEquals(DownloadState.STARTED, dto.getState());
        assertTrue(dto.getCapabilities().isCanPause());
    }

    @Test
    void pause_unknownId_throwsNotFound() {
        assertThrows(DownloadNotFoundException.class, () -> service.pause("unknown"));
    }

    @Test
    void restart_pausedDownload_createsFreshClientAndResumes() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        service.pause(id);
        BtClient restartedClient = mock(BtClient.class);
        CompletableFuture<Void> restartedFuture = new CompletableFuture<>();
        when(clientFactory.create(any(Config.class), any(Path.class), any(String.class))).thenReturn(restartedClient);
        doReturn(restartedFuture).when(restartedClient).startAsync(any(), anyLong());

        service.restart(id);

        assertEquals(DownloadState.STARTED, service.getDownload(id).mapToDownloadDto().getState());
        assertSame(restartedClient, service.getDownload(id).getClient());
        verify(clientFactory, times(2)).create(any(Config.class),
                eq(recordStore.payloadDirectory(root, id)), eq("magnet:?xt=urn:btih:abc%20def"));
    }

    @Test
    void restart_notPausedOrFailed_throwsInvalidState() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);

        assertThrows(InvalidDownloadStateException.class, () -> service.restart(id));
    }

    @Test
    void restart_afterProcessRestart_recoversFromDiskAsPausedThenResumes() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        BitTorrentService recoveredService = new BitTorrentService(directoryService, clientFactory, recordStore);

        DownloadDto recoveredDto = recoveredService.getAllDownloadDtos().iterator().next();
        assertEquals(DownloadState.PAUSED, recoveredDto.getState());

        recoveredService.restart(id);

        assertEquals(DownloadState.STARTED, recoveredService.getDownload(id).mapToDownloadDto().getState());
    }

    @Test
    void stopAndDelete_runningDownload_stopsClientAndDeletesRecordAndStaging() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);

        service.stopAndDelete(id);

        verify(client).stop();
        assertNull(service.getDownload(id));
        assertFalse(Files.exists(recordStore.recordDirectory(root, id)));
    }

    @Test
    void stopAndDelete_finishedDownload_throwsInvalidState() {
        Path finalTarget = root.resolve("Movies").resolve("Movie (2024)");
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();
        service.startDownload(request(MAGNET_LINK), finalTarget);
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        verify(client).startAsync(callbackCaptor.capture(), eq(100L));
        callbackCaptor.getValue().accept(state(4, 4));
        future.complete(null);

        assertThrows(InvalidDownloadStateException.class, () -> service.stopAndDelete(id));
    }

    @Test
    void stopAndDelete_missingFinishedFilesConvertsToFailedAndCleansOnlyOwnedManifestFiles() throws IOException {
        Path sharedDir = root.resolve("Movies").resolve("Shared");
        Files.createDirectories(sharedDir);
        Files.writeString(sharedDir.resolve("sibling.mkv"), "keep-me");
        Files.writeString(sharedDir.resolve("owned.mkv"), "delete-me");
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        DownloadRequest downloadRequest = new DownloadRequest(null, torrentEntry(MAGNET_LINK), null, null);
        DownloadRecord record = DownloadRecord.builder()
                .version(DownloadRecord.CURRENT_VERSION)
                .id(id)
                .state(DownloadRecordState.FINISHED)
                .downloadRequest(downloadRequest)
                .finalTargetRelativePath(recordStore.toRootRelative(root, sharedDir))
                .startTimeInMs(System.currentTimeMillis())
                .finalPayloadManifest(List.of(
                        recordStore.toRootRelative(root, sharedDir.resolve("owned.mkv")),
                        recordStore.toRootRelative(root, sharedDir.resolve("missing.mkv"))))
                .build();
        recordStore.writeRecord(root, record);
        BitTorrentService recoveredService = new BitTorrentService(directoryService, clientFactory, recordStore);
        DownloadDto recoveredDto = recoveredService.getAllDownloadDtos().iterator().next();
        assertEquals(DownloadState.FAILED, recoveredDto.getState());

        recoveredService.stopAndDelete(id);

        assertFalse(Files.exists(sharedDir.resolve("owned.mkv")));
        assertTrue(Files.exists(sharedDir.resolve("sibling.mkv")));
        assertTrue(Files.exists(sharedDir));
        assertNull(recoveredService.getDownload(id));
    }

    @Test
    void remove_finishedDownload_deletesMetadataButKeepsFinalFiles() {
        Path finalTarget = root.resolve("Movies").resolve("Movie (2024)");
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();
        service.startDownload(request(MAGNET_LINK), finalTarget);
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        verify(client).startAsync(callbackCaptor.capture(), eq(100L));
        callbackCaptor.getValue().accept(state(4, 4));
        future.complete(null);

        service.remove(id);

        assertNull(service.getDownload(id));
        assertFalse(Files.exists(recordStore.recordDirectory(root, id)));
    }

    @Test
    void remove_notFinished_throwsInvalidState() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);

        assertThrows(InvalidDownloadStateException.class, () -> service.remove(id));
    }

    @Test
    void shutdown_persistsRunningDownloadsAsPausedAndStopsClients() throws IOException {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);

        service.shutdown();

        verify(client).stop();
        assertEquals(DownloadRecordState.PAUSED, recordStore.readRecord(root, id).getState());
    }

    @Test
    void startupRecovery_convertsStartedRecordToPaused() {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        BitTorrentService freshService = new BitTorrentService(directoryService, clientFactory, recordStore);

        DownloadDto dto = freshService.getAllDownloadDtos().iterator().next();

        assertEquals(DownloadState.PAUSED, dto.getState());
        assertEquals(id, dto.getId());
    }

    @Test
    void startupRecovery_supportsDownloadsWhoseFinalTargetIsRoot() {
        service.startDownload(request(MAGNET_LINK), root);
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        BitTorrentService freshService = new BitTorrentService(directoryService, clientFactory, recordStore);

        DownloadDto dto = freshService.getAllDownloadDtos().iterator().next();

        assertEquals(DownloadState.PAUSED, dto.getState());
        assertEquals(root.toAbsolutePath().normalize(),
                freshService.getDownload(id).getFinalTargetDirectory());
    }

    @Test
    void changingRootPausesOldDownloadsWithoutMovingTheirRecords() throws IOException {
        service.startDownload(request(MAGNET_LINK), root.resolve("target"));
        String id = DownloadIdGenerator.generate(MAGNET_LINK);
        when(directoryService.getRootDirectoryPath()).thenReturn(secondRoot);

        assertTrue(service.getAllDownloadDtos().isEmpty());

        verify(client).stop();
        assertEquals(DownloadRecordState.PAUSED, recordStore.readRecord(root, id).getState());
        assertFalse(Files.exists(recordStore.recordDirectory(secondRoot, id)));
    }

    @Test
    void startupRecovery_invalidRecordBecomesCleanupOnlyFailure() throws IOException {
        String id = "deadbeef";
        Path recordDir = recordStore.recordDirectory(root, id);
        Files.createDirectories(recordDir);
        Files.writeString(recordDir.resolve("download.json"), "not-json");

        DownloadDto dto = service.getAllDownloadDtos().iterator().next();

        assertEquals(DownloadState.FAILED, dto.getState());
        assertEquals(DownloadFailureKind.CLEANUP_ONLY, dto.getFailureKind());
        assertNull(dto.getDownloadRequest());
        assertFalse(dto.getCapabilities().isCanRestart());
        assertTrue(dto.getCapabilities().isCanStopAndDelete());
    }

    private DownloadRequest request(String magnetLink) {
        DownloadRequest request = mock(DownloadRequest.class);
        TorrentEntry torrentEntry = torrentEntry(magnetLink);
        when(request.getTorrentEntry()).thenReturn(torrentEntry);
        return request;
    }

    private TorrentEntry torrentEntry(String magnetLink) {
        TorrentEntry torrentEntry = mock(TorrentEntry.class);
        when(torrentEntry.getMagnetLink()).thenReturn(magnetLink);
        return torrentEntry;
    }

    private TmdbMovieDetailDto movie(String title, String releaseDate) {
        TmdbMovieDetailDto movie = mock(TmdbMovieDetailDto.class);
        when(movie.getTitle()).thenReturn(title);
        when(movie.getRelease_date()).thenReturn(releaseDate);
        return movie;
    }

    private TorrentSessionState state(int piecesComplete, int piecesTotal) {
        TorrentSessionState state = mock(TorrentSessionState.class);
        when(state.getPiecesComplete()).thenReturn(piecesComplete);
        when(state.getPiecesTotal()).thenReturn(piecesTotal);
        when(state.getChunksSizeInBytes()).thenReturn(100L);
        when(state.getConnectedPeers()).thenReturn(Set.of());
        when(state.getSaveTimesOfChunks()).thenReturn(List.<LocalDateTime>of());
        return state;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Consumer<TorrentSessionState>> sessionCallbackCaptor() {
        return ArgumentCaptor.forClass(Consumer.class);
    }
}
