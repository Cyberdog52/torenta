package bt.tracker.http;

import bt.metainfo.TorrentId;
import bt.net.Peer;
import bt.net.PeerId;
import bt.runtime.Config;
import bt.service.IdentityService;
import bt.tracker.TrackerResponse;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpTrackerTest {

    private final List<CloseableHttpClient> clients = new ArrayList<>();
    private final List<ScheduledExecutorService> schedulers = new ArrayList<>();

    @AfterEach
    void tearDown() throws Exception {
        for (CloseableHttpClient client : clients) {
            client.close();
        }
        schedulers.forEach(ScheduledExecutorService::shutdownNow);
    }

    @Test
    void query_encodesBinaryIdentityAndParsesCompactPeers() throws Exception {
        byte[] torrentIdBytes = bytes(0);
        byte[] peerIdBytes = bytes(20);
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/announce", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] body = successResponse();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            IdentityService identityService = mock(IdentityService.class);
            when(identityService.getLocalPeerId()).thenReturn(PeerId.fromBytes(peerIdBytes));
            Config config = new Config();
            config.setAcceptorPort(6891);
            config.setNumberOfPeersToRequestFromTracker(50);
            config.setPeerConnectionTimeout(Duration.ofSeconds(2));
            HttpTracker tracker = tracker(
                    identityService, config,
                    "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
                           + "/announce?passkey=value",
                    address -> true);

            TrackerResponse response = tracker.request(TorrentId.fromBytes(torrentIdBytes)).query();

            assertTrue(response.isSuccess());
            assertEquals(60, response.getInterval());
            assertEquals(1, response.getSeederCount());
            assertEquals(2, response.getLeecherCount());
            List<Peer> peers = new ArrayList<>();
            response.getPeers().forEach(peers::add);
            assertEquals(1, peers.size());
            assertEquals(InetAddress.getByName("127.0.0.1"), peers.get(0).getInetAddress());
            assertEquals(6881, peers.get(0).getPort());
            assertTrue(query.get().startsWith("passkey=value&info_hash=%00%01%02"));
            assertTrue(query.get().contains("peer_id=%14%15%16"));
            assertTrue(query.get().contains("compact=1"));
            assertTrue(query.get().contains("numwant=50"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void query_surfacesTrackerFailureReason() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/announce", exchange -> {
            byte[] body = "d14:failure reason12:tracker downe".getBytes(StandardCharsets.US_ASCII);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            IdentityService identityService = mock(IdentityService.class);
            when(identityService.getLocalPeerId()).thenReturn(PeerId.fromBytes(bytes(20)));
            Config config = new Config();
            config.setPeerConnectionTimeout(Duration.ofSeconds(2));
            HttpTracker tracker = tracker(
                    identityService, config,
                    "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
                           + "/announce",
                    address -> true);

            TrackerResponse response = tracker.request(TorrentId.fromBytes(bytes(0))).query();

            assertEquals("tracker down", response.getErrorMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void query_rejectsNonPublicTrackerAddress() {
        IdentityService identityService = mock(IdentityService.class);
        when(identityService.getLocalPeerId()).thenReturn(PeerId.fromBytes(bytes(20)));
        Config config = new Config();
        config.setPeerConnectionTimeout(Duration.ofSeconds(2));
        HttpTracker tracker = tracker(
                identityService, config, "http://127.0.0.1/announce",
                HttpTrackerFactory::isPublicTrackerAddress);

        TrackerResponse response = tracker.request(TorrentId.fromBytes(bytes(0))).query();

        assertFalse(response.isSuccess());
    }

    @Test
    void addressPolicyRejectsSpecialUseRanges() throws Exception {
        for (String address : List.of(
                "0.0.0.1",
                "10.0.0.1",
                "100.64.0.1",
                "127.0.0.1",
                "169.254.169.254",
                "172.16.0.1",
                "192.0.2.1",
                "192.168.0.1",
                "198.18.0.1",
                "198.51.100.1",
                "203.0.113.1",
                "240.0.0.1",
                "2001:db8::1",
                "fc00::1",
                "fe80::1")) {
            assertFalse(HttpTrackerFactory.isPublicTrackerAddress(InetAddress.getByName(address)), address);
        }
        assertTrue(HttpTrackerFactory.isPublicTrackerAddress(InetAddress.getByName("8.8.8.8")));
        assertTrue(HttpTrackerFactory.isPublicTrackerAddress(InetAddress.getByName("2606:4700:4700::1111")));
    }

    @Test
    void query_rejectsOversizedResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/announce", exchange -> {
            byte[] body = new byte[1_048_577];
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            IdentityService identityService = mock(IdentityService.class);
            when(identityService.getLocalPeerId()).thenReturn(PeerId.fromBytes(bytes(20)));
            Config config = new Config();
            config.setPeerConnectionTimeout(Duration.ofSeconds(2));
            HttpTracker tracker = tracker(
                    identityService, config,
                    "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
                            + "/announce",
                    address -> true);

            TrackerResponse response = tracker.request(TorrentId.fromBytes(bytes(0))).query();

            assertFalse(response.isSuccess());
            assertEquals("Tracker response exceeded size limit", response.getErrorMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void query_followsBoundedRedirect() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", "/announce");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/announce", exchange -> {
            byte[] body = successResponse();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            IdentityService identityService = mock(IdentityService.class);
            when(identityService.getLocalPeerId()).thenReturn(PeerId.fromBytes(bytes(20)));
            Config config = new Config();
            config.setPeerConnectionTimeout(Duration.ofSeconds(2));
            HttpTracker tracker = tracker(
                    identityService, config,
                    "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
                            + "/redirect",
                    address -> true);

            TrackerResponse response = tracker.request(TorrentId.fromBytes(bytes(0))).query();

            assertTrue(response.isSuccess());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void query_abortsStalledResponseBodyAtRequestDeadline() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/announce", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try {
                Thread.sleep(500);
                exchange.getResponseBody().write(successResponse());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            IdentityService identityService = mock(IdentityService.class);
            when(identityService.getLocalPeerId()).thenReturn(PeerId.fromBytes(bytes(20)));
            Config config = new Config();
            config.setPeerConnectionTimeout(Duration.ofMillis(50));
            HttpTracker tracker = tracker(
                    identityService, config,
                    "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort()
                            + "/announce",
                    address -> true);

            TrackerResponse response = assertTimeoutPreemptively(
                    Duration.ofSeconds(1),
                    () -> tracker.request(TorrentId.fromBytes(bytes(0))).query());

            assertFalse(response.isSuccess());
        } finally {
            server.stop(0);
        }
    }

    private byte[] successResponse() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("d8:completei1e10:incompletei2e8:intervali60e5:peers6:"
                .getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(new byte[]{127, 0, 0, 1, 0x1a, (byte) 0xe1});
        out.write('e');
        return out.toByteArray();
    }

    private HttpTracker tracker(IdentityService identityService, Config config, String url,
                                Predicate<InetAddress> addressPolicy) {
        CloseableHttpClient client = HttpTrackerFactory.createHttpClient(addressPolicy);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        clients.add(client);
        schedulers.add(scheduler);
        return new HttpTracker(identityService, config, client, scheduler, url);
    }

    private byte[] bytes(int start) {
        byte[] result = new byte[20];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (start + i);
        }
        return result;
    }
}
