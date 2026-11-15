/*
 * Copyright (c) 2016—2017 Andrei Tomashpolskiy and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bt.net;

import bt.CountingThreadFactory;
import bt.metainfo.TorrentId;
import bt.runtime.Config;
import bt.service.IRuntimeLifecycleBinder;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public class ConnectionSource implements IConnectionSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionSource.class);

    private final IPeerConnectionFactory connectionFactory;
    private final IPeerConnectionPool connectionPool;
    private final ExecutorService connectionExecutor;
    private final Config config;

    private final Map<ConnectionKey, CompletableFuture<ConnectionResult>> pendingConnections;
    private final ConcurrentMap<PeerEndpoint, RetryState> peerRetries;
    private final AtomicLong nextRetryCleanupMillis;
    private final LongSupplier currentTimeMillis;

    @Inject
    public ConnectionSource(Set<PeerConnectionAcceptor> connectionAcceptors,
                            IPeerConnectionFactory connectionFactory,
                            IPeerConnectionPool connectionPool,
                            IRuntimeLifecycleBinder lifecycleBinder,
                            Config config) {
        this(connectionAcceptors, connectionFactory, connectionPool, lifecycleBinder, config,
                System::currentTimeMillis);
    }

    ConnectionSource(Set<PeerConnectionAcceptor> connectionAcceptors,
                     IPeerConnectionFactory connectionFactory,
                     IPeerConnectionPool connectionPool,
                     IRuntimeLifecycleBinder lifecycleBinder,
                     Config config,
                     LongSupplier currentTimeMillis) {

        this.connectionFactory = connectionFactory;
        this.connectionPool = connectionPool;
        this.config = config;
        this.currentTimeMillis = currentTimeMillis;

        this.connectionExecutor = Executors.newFixedThreadPool(
                config.getMaxPendingConnectionRequests(),
                CountingThreadFactory.daemonFactory("bt.net.pool.connection-worker"));
        lifecycleBinder.onShutdown("Shutdown connection workers", connectionExecutor::shutdownNow);

        this.pendingConnections = new ConcurrentHashMap<>();
        this.peerRetries = new ConcurrentHashMap<>();
        this.nextRetryCleanupMillis = new AtomicLong();

        IncomingConnectionListener incomingListener =
                new IncomingConnectionListener(connectionAcceptors, connectionExecutor, connectionPool, config);
        lifecycleBinder.onStartup("Initialize incoming connection acceptors", incomingListener::startup);
        lifecycleBinder.onShutdown("Shutdown incoming connection acceptors", incomingListener::shutdown);
    }

    @Override
    public ConnectionResult getConnection(Peer peer, TorrentId torrentId) {
        try {
            return getConnectionAsync(peer, torrentId).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ConnectionResult.failure("Interrupted while waiting for connection", e);
        } catch (ExecutionException e) {
            return ConnectionResult.failure("Failed to establish connection due to error", e);
        }
    }

    @Override
    public CompletableFuture<ConnectionResult> getConnectionAsync(Peer peer, TorrentId torrentId) {
        ConnectionKey key = new ConnectionKey(peer, peer.getPort(), torrentId);
        PeerEndpoint endpoint = new PeerEndpoint(peer.getInetAddress(), peer.getPort(), torrentId);
        long now = currentTimeMillis.getAsLong();
        cleanExpiredRetryStates(now);

        CompletableFuture<ConnectionResult> connection = getExistingOrPendingConnection(key);
        if (connection != null) {
            if (connection.isDone() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Returning existing connection for peer: {}. Torrent: {}", peer, torrentId);
            }
            return connection;
        }

        RetryState retryState = peerRetries.get(endpoint);
        if (retryState != null && now < retryState.retryAtMillis()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Will not attempt to establish connection to peer: {}. "
                                + "Reason: retry backoff is active until {}. Torrent: {}",
                        peer, retryState.retryAtMillis(), torrentId);
            }
            return CompletableFuture.completedFuture(ConnectionResult.failure("Peer retry backoff is active"));
        }

        if (connectionPool.size() >= config.getMaxPeerConnections()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Will not attempt to establish connection to peer: {}. " +
                        "Reason: connections limit exceeded. Torrent: {}", peer, torrentId);
            }
            return CompletableFuture.completedFuture(ConnectionResult.failure("Connections limit exceeded"));
        }

        synchronized (pendingConnections) {
            connection = getExistingOrPendingConnection(key);
            if (connection != null) {
                if (connection.isDone() && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Returning existing connection for peer: {}. Torrent: {}", peer, torrentId);
                }
                return connection;
            }

            CompletableFuture<ConnectionResult> pending = new CompletableFuture<>();
            pendingConnections.put(key, pending);
            try {
                connectionExecutor.execute(() -> {
                    try {
                        ConnectionResult connectionResult =
                                connectionFactory.createOutgoingConnection(peer, torrentId);
                        if (connectionResult.isSuccess()) {
                            PeerConnection established = connectionResult.getConnection();
                            PeerConnection added = connectionPool.addConnectionIfAbsent(established);
                            if (added != established) {
                                established.closeQuietly();
                            }
                            connectionResult = ConnectionResult.success(added);
                        }
                        updateRetryState(endpoint, peer, connectionResult, null);
                        pendingConnections.remove(key, pending);
                        pending.complete(connectionResult);
                    } catch (Throwable throwable) {
                        updateRetryState(endpoint, peer, null, throwable);
                        pendingConnections.remove(key, pending);
                        pending.completeExceptionally(throwable);
                    }
                });
            } catch (RuntimeException exception) {
                pendingConnections.remove(key, pending);
                pending.completeExceptionally(exception);
            }
            return pending;
        }
    }

    private void updateRetryState(PeerEndpoint endpoint, Peer peer, ConnectionResult result, Throwable throwable) {
        if (throwable == null && result != null && result.isSuccess()) {
            peerRetries.remove(endpoint);
            return;
        }
        RetryState retryState = peerRetries.compute(endpoint, (ignored, previous) -> {
            long now = currentTimeMillis.getAsLong();
            if (previous != null && now < previous.retryAtMillis()) {
                return previous;
            }
            int failures = previous == null ? 1 : previous.failures() + 1;
            long delay = retryDelayMillis(failures);
            long retryAtMillis = addWithoutOverflow(now, delay);
            long expiresAtMillis = addWithoutOverflow(retryAtMillis,
                    config.getUnreachablePeerBanDuration().toMillis());
            return new RetryState(failures, retryAtMillis, expiresAtMillis);
        });
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Peer connection failed: {}. Attempt {} may retry after {}",
                    peer, retryState.failures(), retryState.retryAtMillis(), throwable);
        }
    }

    private void cleanExpiredRetryStates(long now) {
        long cleanupAtMillis = nextRetryCleanupMillis.get();
        if (now < cleanupAtMillis) {
            return;
        }
        long cleanupIntervalMillis = Math.max(1_000, config.getPeerConnectionRetryInterval().toMillis());
        if (nextRetryCleanupMillis.compareAndSet(
                cleanupAtMillis, addWithoutOverflow(now, cleanupIntervalMillis))) {
            peerRetries.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
        }
    }

    private long retryDelayMillis(int failures) {
        if (failures >= config.getPeerConnectionRetryCount()) {
            return config.getUnreachablePeerBanDuration().toMillis();
        }
        long baseDelay = config.getPeerConnectionRetryInterval().toMillis();
        int exponent = Math.min(30, Math.max(0, failures - 1));
        long delay;
        try {
            delay = Math.multiplyExact(baseDelay, 1L << exponent);
        } catch (ArithmeticException exception) {
            delay = Long.MAX_VALUE;
        }
        return Math.min(delay, config.getUnreachablePeerBanDuration().toMillis());
    }

    private long addWithoutOverflow(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private CompletableFuture<ConnectionResult> getExistingOrPendingConnection(ConnectionKey key) {
        PeerConnection existingConnection = connectionPool.getConnection(key);
        if (existingConnection != null) {
            return CompletableFuture.completedFuture(ConnectionResult.success(existingConnection));
        }

        CompletableFuture<ConnectionResult> pendingConnection = pendingConnections.get(key);
        if (pendingConnection != null) {
            return pendingConnection;
        }

        return null;
    }

    private record RetryState(int failures, long retryAtMillis, long expiresAtMillis) {
    }

    private record PeerEndpoint(InetAddress address, int port, TorrentId torrentId) {
    }
}
