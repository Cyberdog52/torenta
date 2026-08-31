package ch.andreskonrad.torenta.bittorrent.service;

import bt.net.ConnectionKey;
import bt.runtime.BtClient;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
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
        Download download = new Download(7, request, Path.of("downloads"), mock(BtClient.class), future);
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

        assertEquals(7, dto.getId());
        assertEquals(DownloadState.STARTED, dto.getState());
        assertEquals(0.75, dto.getProgress());
        assertSame(request, dto.getDownloadRequest());
        assertEquals(download.getStartTimeInMs(), dto.getStartTimeInMs());
        assertEquals(2, dto.getConnectedPeers());
        assertEquals(480, dto.getTotalBytes());
        assertEquals(4.0, dto.getDownloadSpeedInBytesPerSecond());
    }

    @Test
    void mapToDownloadDto_finished_whenFutureCompletes() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Download download = download(1, future);
        download.setState(state(4, 4, 120));
        future.complete(null);

        DownloadDto dto = download.mapToDownloadDto();

        assertTrue(download.isDone());
        assertEquals(DownloadState.FINISHED, dto.getState());
        assertEquals(1.0, dto.getProgress());
    }

    @Test
    void cancel_cancelsFutureAndMapsCancelledState() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Download download = download(1, future);
        download.setState(state(1, 4, 120));

        download.cancel();
        DownloadDto dto = download.mapToDownloadDto();

        assertTrue(future.isCancelled());
        assertTrue(download.isDone());
        assertEquals(DownloadState.CANCELLED, dto.getState());
    }

    @Test
    void setTorrentFuture_afterCancellationCancelsAttachedFuture() {
        CompletableFuture<Void> pendingFuture = new CompletableFuture<>();
        CompletableFuture<Void> torrentFuture = new CompletableFuture<>();
        Download download = download(1, pendingFuture);

        download.cancel();
        download.setTorrentFuture(torrentFuture);

        assertTrue(pendingFuture.isCancelled());
        assertTrue(torrentFuture.isCancelled());
    }

    @Test
    void mapToDownloadDto_mapsNullConnectedPeersAsZero() {
        Download download = download(1, new CompletableFuture<>());
        TorrentSessionState state = state(1, 4, 120);
        when(state.getConnectedPeers()).thenReturn(null);
        download.setState(state);

        assertEquals(0, download.mapToDownloadDto().getConnectedPeers());
    }

    @Test
    void mapToDownloadDto_withoutSessionStateMapsZeroMetrics() {
        Download download = download(1, new CompletableFuture<>());

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.STARTED, dto.getState());
        assertEquals(0, dto.getProgress());
        assertEquals(0, dto.getConnectedPeers());
        assertEquals(0, dto.getTotalBytes());
        assertEquals(0, dto.getDownloadSpeedInBytesPerSecond());
    }

    @Test
    void getters_returnConstructorAndMutableStateValues() {
        DownloadRequest request = new DownloadRequest();
        Path targetDirectory = Path.of("target");
        BtClient client = mock(BtClient.class);
        CompletableFuture<Void> future = new CompletableFuture<>();
        Download download = new Download(9, request, targetDirectory, client, future);
        TorrentSessionState state = mock(TorrentSessionState.class);

        assertEquals(9, download.getId());
        assertSame(request, download.getDownloadRequest());
        assertSame(targetDirectory, download.getTargetDirectory());
        assertSame(client, download.getClient());
        assertNull(download.getState());
        assertFalse(download.isDone());

        download.setState(state);

        assertSame(state, download.getState());
        assertTrue(download.getStartTimeInMs() > 0);
    }

    @Test
    void equalsAndHashCode_useOnlyIdAndHandleAllObjectBranches() {
        Download download = download(3, new CompletableFuture<>());
        Download sameId = download(3, new CompletableFuture<>());
        Download differentId = download(4, new CompletableFuture<>());

        assertTrue(download.equals(download));
        assertTrue(download.equals(sameId));
        assertEquals(download.hashCode(), sameId.hashCode());
        assertFalse(download.equals(differentId));
        assertNotEquals(download.hashCode(), differentId.hashCode());
        assertFalse(download.equals(null));
        assertFalse(download.equals(new Object()));
    }

    @Test
    void fail_mapsFailedStateAndErrorMessage() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Download download = download(1, future);
        download.setState(state(1, 4, 120));

        download.fail("BtException: no peers");
        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.FAILED, dto.getState());
        assertEquals("BtException: no peers", dto.getErrorMessage());
    }

    @Test
    void isComplete_onlyTrueWhenAllPiecesAreDownloaded() {
        Download download = download(1, new CompletableFuture<>());

        assertFalse(download.isComplete());

        download.setState(state(3, 4, 120));
        assertFalse(download.isComplete());

        download.setState(state(4, 4, 120));
        assertTrue(download.isComplete());
    }

    @Test
    void isComplete_falseWhenPieceCountIsUnknown() {
        Download download = download(1, new CompletableFuture<>());
        download.setState(state(0, 0, 120));

        assertFalse(download.isComplete());
    }

    @Test
    void mapToDownloadDto_withoutPeersAfterDiscoveryTimeout_isReportedAsFailed() {
        long startedLongAgo = System.currentTimeMillis() - Download.PEER_DISCOVERY_TIMEOUT_IN_MS - 1000;
        Download download = new Download(1, new DownloadRequest(), Path.of("downloads"),
                mock(BtClient.class), new CompletableFuture<>(), startedLongAgo);
        download.setState(state(0, 0, 120));

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.FAILED, dto.getState());
        assertTrue(dto.getErrorMessage().contains("No peers found"));
    }

    @Test
    void mapToDownloadDto_withPeersAfterDiscoveryTimeout_staysStarted() {
        long startedLongAgo = System.currentTimeMillis() - Download.PEER_DISCOVERY_TIMEOUT_IN_MS - 1000;
        Download download = new Download(1, new DownloadRequest(), Path.of("downloads"),
                mock(BtClient.class), new CompletableFuture<>(), startedLongAgo);
        TorrentSessionState state = state(0, 4, 120);
        when(state.getConnectedPeers()).thenReturn(Set.of(mock(ConnectionKey.class)));
        download.setState(state);
        download.setState(state(0, 4, 120));

        DownloadDto dto = download.mapToDownloadDto();

        assertEquals(DownloadState.STARTED, dto.getState());
        assertNull(dto.getErrorMessage());
    }

    private Download download(int id, CompletableFuture<Void> future) {
        return new Download(id, new DownloadRequest(), Path.of("downloads"), mock(BtClient.class), future);
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
