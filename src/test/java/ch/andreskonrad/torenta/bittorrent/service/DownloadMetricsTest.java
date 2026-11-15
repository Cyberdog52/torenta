package ch.andreskonrad.torenta.bittorrent.service;

import bt.net.ConnectionKey;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadMetricsTest {

    private SimpleMeterRegistry registry;
    private AtomicLong nanoTime;
    private DownloadMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        nanoTime = new AtomicLong();
        metrics = new DownloadMetrics(registry, nanoTime::get);
    }

    @Test
    void stateUpdated_recordsFirstMilestonesOnlyOnceAndUpdatesGauges() {
        TorrentSessionState initial = state(4, 0, Set.of(), 0, 0);
        TorrentSessionState progressing = state(4, 1, Set.of(mock(ConnectionKey.class)), 4096, 512);
        metrics.started("download");

        nanoTime.set(1_000_000_000L);
        metrics.stateUpdated("download", initial);
        nanoTime.set(2_000_000_000L);
        metrics.stateUpdated("download", progressing);
        nanoTime.set(3_000_000_000L);
        metrics.stateUpdated("download", progressing);

        assertEquals(1, registry.get(DownloadMetrics.DOWNLOADS_STARTED).counter().count());
        assertEquals(1, registry.get(DownloadMetrics.TIME_TO_METADATA).timer().count());
        assertEquals(1, registry.get(DownloadMetrics.TIME_TO_FIRST_PEER).timer().count());
        assertEquals(1, registry.get(DownloadMetrics.TIME_TO_FIRST_PIECE).timer().count());
        assertEquals(1, registry.get(DownloadMetrics.ACTIVE_DOWNLOADS).gauge().value());
        assertEquals(1, registry.get(DownloadMetrics.CONNECTED_PEERS).gauge().value());
        assertEquals(4096, registry.get(DownloadMetrics.DOWNLOADED_BYTES).gauge().value());
        assertEquals(512, registry.get(DownloadMetrics.UPLOADED_BYTES).gauge().value());
    }

    @Test
    void restart_recordsRecheckWhenVerifiedProgressCatchesUp() {
        metrics.restarted("download", 0.5);

        nanoTime.set(1_000_000_000L);
        metrics.stateUpdated("download", state(4, 1, Set.of(), 1024, 0));
        nanoTime.set(2_000_000_000L);
        metrics.stateUpdated("download", state(4, 2, Set.of(), 2048, 0));

        assertEquals(1, registry.get(DownloadMetrics.DOWNLOADS_RESTARTED).counter().count());
        assertEquals(1, registry.get(DownloadMetrics.RECHECK_TIME).timer().count());
    }

    @Test
    void lifecycleOutcomesRemoveActiveObservationAndRecordCounters() {
        metrics.started("finished");
        nanoTime.set(2_000_000_000L);
        metrics.finished("finished");
        metrics.started("paused");
        metrics.paused("paused");
        metrics.started("failed");
        metrics.failed("failed", DownloadFailureKind.RESTARTABLE);
        metrics.started("removed");
        metrics.removed("removed");

        assertEquals(1, registry.get(DownloadMetrics.DOWNLOADS_FINISHED).counter().count());
        assertEquals(1, registry.get(DownloadMetrics.DOWNLOADS_PAUSED).counter().count());
        assertEquals(1, registry.get(DownloadMetrics.DOWNLOADS_FAILED)
                .tag("kind", "restartable").counter().count());
        assertEquals(1, registry.get(DownloadMetrics.TIME_TO_COMPLETION).timer().count());
        assertEquals(0, registry.get(DownloadMetrics.ACTIVE_DOWNLOADS).gauge().value());
    }

    @Test
    void disabledMetrics_acceptLifecycleEvents() {
        DownloadMetrics disabled = DownloadMetrics.disabled();

        disabled.started("download");
        disabled.stateUpdated("download", state(0, 0, null, -1, -1));
        disabled.restarted("download", 0.5);
        disabled.paused("download");
        disabled.failed("download", DownloadFailureKind.CLEANUP_ONLY);
        disabled.finished("download");
        disabled.removed("download");
    }

    private TorrentSessionState state(int piecesTotal, int piecesComplete, Set<ConnectionKey> peers,
                                      long downloaded, long uploaded) {
        TorrentSessionState state = mock(TorrentSessionState.class);
        when(state.getPiecesTotal()).thenReturn(piecesTotal);
        when(state.getPiecesComplete()).thenReturn(piecesComplete);
        when(state.getConnectedPeers()).thenReturn(peers);
        when(state.getDownloaded()).thenReturn(downloaded);
        when(state.getUploaded()).thenReturn(uploaded);
        return state;
    }
}
