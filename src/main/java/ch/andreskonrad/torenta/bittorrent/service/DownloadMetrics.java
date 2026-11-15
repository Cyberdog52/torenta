package ch.andreskonrad.torenta.bittorrent.service;

import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadFailureKind;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * Low-cardinality download metrics. Download ids are used only as in-memory correlation keys and
 * are never emitted as metric tags.
 */
@Component
public class DownloadMetrics {

    static final String ACTIVE_DOWNLOADS = "torenta.bittorrent.downloads.active";
    static final String CONNECTED_PEERS = "torenta.bittorrent.peers.connected";
    static final String DOWNLOADED_BYTES = "torenta.bittorrent.bytes.downloaded";
    static final String UPLOADED_BYTES = "torenta.bittorrent.bytes.uploaded";
    static final String DOWNLOADS_STARTED = "torenta.bittorrent.downloads.started";
    static final String DOWNLOADS_PAUSED = "torenta.bittorrent.downloads.paused";
    static final String DOWNLOADS_RESTARTED = "torenta.bittorrent.downloads.restarted";
    static final String DOWNLOADS_FINISHED = "torenta.bittorrent.downloads.finished";
    static final String DOWNLOADS_FAILED = "torenta.bittorrent.downloads.failed";
    static final String TIME_TO_METADATA = "torenta.bittorrent.time.to.metadata";
    static final String TIME_TO_FIRST_PEER = "torenta.bittorrent.time.to.first.peer";
    static final String TIME_TO_FIRST_PIECE = "torenta.bittorrent.time.to.first.piece";
    static final String TIME_TO_COMPLETION = "torenta.bittorrent.time.to.completion";
    static final String RECHECK_TIME = "torenta.bittorrent.time.to.recheck";

    private final ConcurrentMap<String, Observation> observations = new ConcurrentHashMap<>();
    private final LongSupplier nanoTime;
    private final Counter started;
    private final Counter paused;
    private final Counter restarted;
    private final Counter finished;
    private final Map<DownloadFailureKind, Counter> failed;
    private final Timer timeToMetadata;
    private final Timer timeToFirstPeer;
    private final Timer timeToFirstPiece;
    private final Timer timeToCompletion;
    private final Timer recheckTime;
    private final boolean enabled;

    private DownloadMetrics() {
        this.nanoTime = System::nanoTime;
        this.started = null;
        this.paused = null;
        this.restarted = null;
        this.finished = null;
        this.failed = Map.of();
        this.timeToMetadata = null;
        this.timeToFirstPeer = null;
        this.timeToFirstPiece = null;
        this.timeToCompletion = null;
        this.recheckTime = null;
        this.enabled = false;
    }

    @Autowired
    public DownloadMetrics(MeterRegistry registry) {
        this(registry, System::nanoTime);
    }

    DownloadMetrics(MeterRegistry registry, LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
        this.started = registry.counter(DOWNLOADS_STARTED);
        this.paused = registry.counter(DOWNLOADS_PAUSED);
        this.restarted = registry.counter(DOWNLOADS_RESTARTED);
        this.finished = registry.counter(DOWNLOADS_FINISHED);
        this.failed = Map.of(
                DownloadFailureKind.RESTARTABLE, failureCounter(registry, DownloadFailureKind.RESTARTABLE),
                DownloadFailureKind.CLEANUP_ONLY, failureCounter(registry, DownloadFailureKind.CLEANUP_ONLY));
        this.timeToMetadata = registry.timer(TIME_TO_METADATA);
        this.timeToFirstPeer = registry.timer(TIME_TO_FIRST_PEER);
        this.timeToFirstPiece = registry.timer(TIME_TO_FIRST_PIECE);
        this.timeToCompletion = registry.timer(TIME_TO_COMPLETION);
        this.recheckTime = registry.timer(RECHECK_TIME);
        this.enabled = true;

        Gauge.builder(ACTIVE_DOWNLOADS, observations, ConcurrentMap::size).register(registry);
        Gauge.builder(CONNECTED_PEERS, observations, DownloadMetrics::connectedPeers).register(registry);
        Gauge.builder(DOWNLOADED_BYTES, observations, DownloadMetrics::downloadedBytes)
                .baseUnit("bytes")
                .register(registry);
        Gauge.builder(UPLOADED_BYTES, observations, DownloadMetrics::uploadedBytes)
                .baseUnit("bytes")
                .register(registry);
    }

    static DownloadMetrics disabled() {
        return new DownloadMetrics();
    }

    public void started(String id) {
        if (!enabled) {
            return;
        }
        observations.put(id, new Observation(nanoTime.getAsLong(), 0));
        started.increment();
    }

    public void paused(String id) {
        if (!enabled) {
            return;
        }
        observations.remove(id);
        paused.increment();
    }

    public void restarted(String id, double recoveredProgress) {
        if (!enabled) {
            return;
        }
        observations.put(id, new Observation(nanoTime.getAsLong(), recoveredProgress));
        restarted.increment();
    }

    public void stateUpdated(String id, TorrentSessionState state) {
        if (!enabled) {
            return;
        }
        Observation observation = observations.get(id);
        if (observation != null) {
            observation.update(state, nanoTime.getAsLong(), timeToMetadata, timeToFirstPeer,
                    timeToFirstPiece, recheckTime);
        }
    }

    public void finished(String id) {
        if (!enabled) {
            return;
        }
        Observation observation = observations.remove(id);
        if (observation != null) {
            timeToCompletion.record(Duration.ofNanos(elapsedSince(observation.startedAtNanos)));
        }
        finished.increment();
    }

    public void failed(String id, DownloadFailureKind failureKind) {
        if (!enabled) {
            return;
        }
        observations.remove(id);
        Counter failureCounter = failed.get(failureKind);
        if (failureCounter != null) {
            failureCounter.increment();
        }
    }

    public void removed(String id) {
        observations.remove(id);
    }

    private long elapsedSince(long startedAtNanos) {
        return Math.max(0, nanoTime.getAsLong() - startedAtNanos);
    }

    private static Counter failureCounter(MeterRegistry registry, DownloadFailureKind failureKind) {
        return Counter.builder(DOWNLOADS_FAILED)
                .tag("kind", failureKind.name().toLowerCase(Locale.ROOT))
                .register(registry);
    }

    private static double connectedPeers(ConcurrentMap<String, Observation> observations) {
        return observations.values().stream().mapToInt(Observation::connectedPeers).sum();
    }

    private static double downloadedBytes(ConcurrentMap<String, Observation> observations) {
        return observations.values().stream().mapToLong(Observation::downloadedBytes).sum();
    }

    private static double uploadedBytes(ConcurrentMap<String, Observation> observations) {
        return observations.values().stream().mapToLong(Observation::uploadedBytes).sum();
    }

    private static final class Observation {
        private final long startedAtNanos;
        private final double recoveredProgress;
        private boolean metadataSeen;
        private boolean peerSeen;
        private boolean pieceSeen;
        private boolean recheckFinished;
        private volatile int connectedPeers;
        private volatile long downloadedBytes;
        private volatile long uploadedBytes;

        private Observation(long startedAtNanos, double recoveredProgress) {
            this.startedAtNanos = startedAtNanos;
            this.recoveredProgress = recoveredProgress;
            this.recheckFinished = recoveredProgress <= 0;
        }

        private synchronized void update(TorrentSessionState state, long now, Timer metadataTimer,
                                         Timer peerTimer, Timer pieceTimer, Timer recheckTimer) {
            connectedPeers = safeSize(state.getConnectedPeers());
            downloadedBytes = Math.max(0, state.getDownloaded());
            uploadedBytes = Math.max(0, state.getUploaded());
            Duration elapsed = Duration.ofNanos(Math.max(0, now - startedAtNanos));

            if (!metadataSeen && state.getPiecesTotal() > 0) {
                metadataSeen = true;
                metadataTimer.record(elapsed);
            }
            if (!peerSeen && connectedPeers > 0) {
                peerSeen = true;
                peerTimer.record(elapsed);
            }
            if (!pieceSeen && state.getPiecesComplete() > 0) {
                pieceSeen = true;
                pieceTimer.record(elapsed);
            }
            if (!recheckFinished && hasRecoveredProgress(state)) {
                recheckFinished = true;
                recheckTimer.record(elapsed);
            }
        }

        private boolean hasRecoveredProgress(TorrentSessionState state) {
            return state.getPiecesTotal() > 0
                    && ((double) state.getPiecesComplete()) / state.getPiecesTotal() >= recoveredProgress;
        }

        private int connectedPeers() {
            return connectedPeers;
        }

        private long downloadedBytes() {
            return downloadedBytes;
        }

        private long uploadedBytes() {
            return uploadedBytes;
        }

        private static int safeSize(Set<?> values) {
            return values == null ? 0 : values.size();
        }
    }
}
