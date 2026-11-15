package bt.net;

import bt.metainfo.TorrentId;
import bt.runtime.Config;
import bt.service.IRuntimeLifecycleBinder;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionSourceTest {

    @Test
    void failedConnectionsUseConfiguredBackoffAndRetryWithoutCachingCompletedFuture() {
        List<Runnable> shutdownHooks = new ArrayList<>();
        IRuntimeLifecycleBinder lifecycleBinder = mock(IRuntimeLifecycleBinder.class);
        doAnswer(invocation -> {
            shutdownHooks.add(invocation.getArgument(1));
            return null;
        }).when(lifecycleBinder).onShutdown(anyString(), any(Runnable.class));

        Config config = new Config();
        config.setMaxPendingConnectionRequests(1);
        config.setMaxPeerConnections(10);
        config.setPeerConnectionRetryInterval(Duration.ofMillis(100));
        config.setPeerConnectionRetryCount(2);
        config.setUnreachablePeerBanDuration(Duration.ofSeconds(1));

        IPeerConnectionFactory connectionFactory = mock(IPeerConnectionFactory.class);
        IPeerConnectionPool connectionPool = mock(IPeerConnectionPool.class);
        when(connectionFactory.createOutgoingConnection(any(), any()))
                .thenReturn(ConnectionResult.failure("unreachable", new IllegalStateException("first")))
                .thenReturn(ConnectionResult.failure("unreachable", new IllegalStateException("second")))
                .thenReturn(ConnectionResult.success(mock(PeerConnection.class)));
        when(connectionPool.addConnectionIfAbsent(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicLong time = new AtomicLong();
        ConnectionSource source = new ConnectionSource(
                Set.of(mock(PeerConnectionAcceptor.class)),
                connectionFactory,
                connectionPool,
                lifecycleBinder,
                config,
                time::get);
        Peer peer = mock(Peer.class);
        when(peer.getInetAddress()).thenReturn(InetAddress.getLoopbackAddress());
        when(peer.getPort()).thenReturn(6881);
        Peer rediscoveredPeer = mock(Peer.class);
        when(rediscoveredPeer.getInetAddress()).thenReturn(InetAddress.getLoopbackAddress());
        when(rediscoveredPeer.getPort()).thenReturn(6881);
        TorrentId torrentId = TorrentId.fromBytes(new byte[20]);

        try {
            assertFalse(source.getConnection(peer, torrentId).isSuccess());
            assertFalse(source.getConnection(rediscoveredPeer, torrentId).isSuccess());
            verify(connectionFactory).createOutgoingConnection(peer, torrentId);

            time.set(100);
            assertFalse(source.getConnection(rediscoveredPeer, torrentId).isSuccess());
            verify(connectionFactory).createOutgoingConnection(rediscoveredPeer, torrentId);
            verify(connectionFactory, times(2)).createOutgoingConnection(any(), any());

            time.set(1_099);
            assertFalse(source.getConnection(peer, torrentId).isSuccess());
            verify(connectionFactory, times(2)).createOutgoingConnection(any(), any());

            time.set(1_100);
            assertTrue(source.getConnection(peer, torrentId).isSuccess());
            verify(connectionFactory, times(3)).createOutgoingConnection(any(), any());
        } finally {
            shutdownHooks.forEach(Runnable::run);
        }
    }
}
