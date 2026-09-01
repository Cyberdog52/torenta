package ch.andreskonrad.torenta.bittorrent.service;

import bt.net.ConnectionKey;
import bt.runtime.BtClient;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRecordState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadTest {

    @Test
    void mapToDownloadDto_started_mapsProgressBytesPeersAndRecentChunkSpeed() {
        DownloadRequest request = new DownloadRequest();
        CompletableFuture<Void> future = new CompletableFuture<>();
        Download download = new Download("7", request, Path.of("staging"), Path.of("final"), mock(BtClient.class), future);
        TorrentSessionState state = state(3, 4, 120);
        ConnectionKey firstPeer = mock(ConnectionKey.class);
        ConnectionKey secondPeer = mock(ConnectionKey.class);
        when(state.getConnectedPeers()).thenReturn(Set.of(firstPeer, secondPeer));
        when(state.getSaveTimesOfChunks()).thenReturn(List.of(
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().minusHours(1)));
        download.setState(state);

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals("7", dto.getId());
        assertEquals(DownloadState.STARTED, dto.getState());
        assertNull(dto.getFailureKind());
        assertEquals(0.75, dto.getProgress());
        assertSame(request, dto.getDownloadRequest());
        assertEquals(download.getStartTimeInMs(), dto.getStartTimeInMs());
        assertEquals(2, dto.getConnectedPeers());
        assertEquals(480, dto.getTotalBytes());
        assertEquals(4.0, dto.getDownloadSpeedInBytesPerSecond());
        assertTrue(dto.getCapabilities().isCanPause());
        assertFalse(dto.getCapabilities().isCanRestart());
        assertTrue(dto.getCapabilities().isCanStopAndDelete());
        assertFalse(dto.getCapabilities().isCanRemove());
    }

    @Test
    void mapToDownloadDto_finished_reportsFullProgressAndRemoveOnlyCapability() {
        Download download = download("1");
        download.setState(state(4, 4, 120));
        download.markFinished(List.of("Movies/Movie (2024)/movie.mkv"));

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.FINISHED, dto.getState());
        assertEquals(1.0, dto.getProgress());
        assertNull(dto.getErrorMessage());
        assertFalse(dto.getCapabilities().isCanPause());
        assertFalse(dto.getCapabilities().isCanRestart());
        assertFalse(dto.getCapabilities().isCanStopAndDelete());
        assertTrue(dto.getCapabilities().isCanRemove());
    }

    @Test
    void beginPauseAndMarkPaused_reportsPausedStateAndRestartCapability() {
        Download download = download("1");
        download.setState(state(1, 4, 120));

        download.beginPause();
        download.markPaused();
        DownloadDto dto = download.mapToDownloadDto();

        assertTrue(download.isPaused());
        assertEquals(DownloadState.PAUSED, dto.getState());
        assertTrue(dto.getCapabilities().isCanRestart());
        assertTrue(dto.getCapabilities().isCanStopAndDelete());
        assertFalse(dto.getCapabilities().isCanPause());
    }

    @Test
    void restart_rebindsClientAndFutureAndResetsToStarted() {
        Download download = download("1");
        download.beginPause();
        download.markPaused();
        BtClient newClient = mock(BtClient.class);
        CompletableFuture<Void> newFuture = new CompletableFuture<>();

        download.restart(newClient, newFuture);

        assertFalse(download.isPaused());
        assertEquals(DownloadRecordState.STARTED, download.getRecordState());
        assertSame(newClient, download.getClient());
        assertFalse(download.isDone());
        newFuture.complete(null);
        assertTrue(download.isDone());
    }

    @Test
    void fail_restartable_mapsFailedStateWithRestartCapability() {
        Download download = download("1");
        download.setState(state(1, 4, 120));

        download.fail("BtException: no peers", DownloadFailureKind.RESTARTABLE);
        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.FAILED, dto.getState());
        assertEquals(DownloadFailureKind.RESTARTABLE, dto.getFailureKind());
        assertEquals("BtException: no peers", dto.getErrorMessage());
        assertTrue(dto.getCapabilities().isCanRestart());
        assertTrue(dto.getCapabilities().isCanStopAndDelete());
    }

    @Test
    void fail_cleanupOnly_mapsFailedStateWithoutRestartCapability() {
        Download download = download("1");

        download.fail("Cleanup could not remove all owned files.", DownloadFailureKind.CLEANUP_ONLY);
        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.FAILED, dto.getState());
        assertEquals(DownloadFailureKind.CLEANUP_ONLY, dto.getFailureKind());
        assertFalse(dto.getCapabilities().isCanRestart());
        assertTrue(dto.getCapabilities().isCanStopAndDelete());
    }

    @Test
    void isComplete_onlyTrueWhenAllPiecesAreDownloaded() {
        Download download = download("1");

        assertFalse(download.isComplete());

        download.setState(state(3, 4, 120));
        assertFalse(download.isComplete());

        download.setState(state(4, 4, 120));
        assertTrue(download.isComplete());
    }

    @Test
    void isComplete_falseWhenPieceCountIsUnknown() {
        Download download = download("1");
        download.setState(state(0, 0, 120));

        assertFalse(download.isComplete());
    }

    @Test
    void recovered_usesPersistedProgressAndTotalBytesWithoutLiveState() {
        Download download = Download.recovered("1", new DownloadRequest(), Path.of("staging"), Path.of("final"),
                1234L, DownloadRecordState.PAUSED, null, null, 0.5, 2048L, List.of());

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.PAUSED, dto.getState());
        assertEquals(0.5, dto.getProgress());
        assertEquals(2048L, dto.getTotalBytes());
        assertEquals(0, dto.getConnectedPeers());
        assertEquals(0, dto.getDownloadSpeedInBytesPerSecond());
        assertTrue(download.isPaused());
    }

    @Test
    void recovered_invalidRecordHasNoRequestAndOnlyCleanupCapability() {
        Download download = Download.recovered("1", null, Path.of("staging"), null,
                0L, DownloadRecordState.FAILED, DownloadFailureKind.CLEANUP_ONLY, "Invalid download record", 0, 0,
                List.of());

        DownloadDto dto = download.mapToDownloadDto();

        assertNull(dto.getDownloadRequest());
        assertEquals(DownloadState.FAILED, dto.getState());
        assertEquals(DownloadFailureKind.CLEANUP_ONLY, dto.getFailureKind());
        assertFalse(dto.getCapabilities().isCanRestart());
        assertTrue(dto.getCapabilities().isCanStopAndDelete());
    }

    @Test
    void getters_returnConstructorAndMutableStateValues() {
        DownloadRequest request = new DownloadRequest();
        Path stagingDirectory = Path.of("staging");
        Path finalTargetDirectory = Path.of("final");
        BtClient client = mock(BtClient.class);
        CompletableFuture<Void> future = new CompletableFuture<>();
        Download download = new Download("9", request, stagingDirectory, finalTargetDirectory, client, future);
        TorrentSessionState state = mock(TorrentSessionState.class);

        assertEquals("9", download.getId());
        assertSame(request, download.getDownloadRequest());
        assertSame(stagingDirectory, download.getStagingDirectory());
        assertSame(finalTargetDirectory, download.getFinalTargetDirectory());
        assertSame(client, download.getClient());
        assertNull(download.getState());
        assertFalse(download.isDone());

        download.setState(state);

        assertSame(state, download.getState());
        assertTrue(download.getStartTimeInMs() > 0);
    }

    @Test
    void equalsAndHashCode_useOnlyIdAndHandleAllObjectBranches() {
        Download download = download("3");
        Download sameId = download("3");
        Download differentId = download("4");

        assertTrue(download.equals(download));
        assertTrue(download.equals(sameId));
        assertEquals(download.hashCode(), sameId.hashCode());
        assertFalse(download.equals(differentId));
        assertNotEquals(download.hashCode(), differentId.hashCode());
        assertFalse(download.equals(null));
        assertFalse(download.equals(new Object()));
    }

    @Test
    void mapToDownloadDto_withoutPeersAfterDiscoveryTimeout_isReportedAsFailed() {
        long startedLongAgo = System.currentTimeMillis() - Download.PEER_DISCOVERY_TIMEOUT_IN_MS - 1000;
        Download download = new Download("1", new DownloadRequest(), Path.of("staging"), Path.of("final"),
                mock(BtClient.class), new CompletableFuture<>(), startedLongAgo);
        download.setState(state(0, 0, 120));

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.FAILED, dto.getState());
        assertEquals(DownloadFailureKind.RESTARTABLE, dto.getFailureKind());
        assertTrue(dto.getErrorMessage().contains("No peers found"));
    }

    @Test
    void mapToDownloadDto_withPeersAfterDiscoveryTimeout_staysStarted() {
        long startedLongAgo = System.currentTimeMillis() - Download.PEER_DISCOVERY_TIMEOUT_IN_MS - 1000;
        Download download = new Download("1", new DownloadRequest(), Path.of("staging"), Path.of("final"),
                mock(BtClient.class), new CompletableFuture<>(), startedLongAgo);
        TorrentSessionState state = state(0, 4, 120);
        when(state.getConnectedPeers()).thenReturn(Set.of(mock(ConnectionKey.class)));
        download.setState(state);
        download.setState(state(0, 4, 120));

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.STARTED, dto.getState());
        assertNull(dto.getErrorMessage());
    }

    @Test
    void restart_resetsPeerDiscoveryTimeoutForOldDownload() {
        long startedLongAgo = System.currentTimeMillis() - Download.PEER_DISCOVERY_TIMEOUT_IN_MS - 1000;
        Download download = new Download("1", new DownloadRequest(), Path.of("staging"), Path.of("final"),
                mock(BtClient.class), CompletableFuture.completedFuture(null), startedLongAgo);

        download.restart(mock(BtClient.class), new CompletableFuture<>());

        DownloadDto dto = download.mapToDownloadDto();
        assertEquals(DownloadState.STARTED, dto.getState());
        assertNull(dto.getErrorMessage());
    }

    private Download download(String id) {
        return new Download(id, new DownloadRequest(), Path.of("staging"), Path.of("final"), mock(BtClient.class), new CompletableFuture<>());
    }

    private TorrentSessionState state(int piecesComplete, int piecesTotal, long chunkSize) {
        TorrentSessionState state = mock(TorrentSessionState.class);
        when(state.getPiecesComplete()).thenReturn(piecesComplete);
        when(state.getPiecesTotal()).thenReturn(piecesTotal);
        when(state.getChunksSizeInBytes()).thenReturn(chunkSize);
        when(state.getConnectedPeers()).thenReturn(Set.of());
        when(state.getSaveTimesOfChunks()).thenReturn(List.of());
        return state;
    }
}
