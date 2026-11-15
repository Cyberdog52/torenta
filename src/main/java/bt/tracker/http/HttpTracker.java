package bt.tracker.http;

import bt.BtException;
import bt.bencoding.BEParser;
import bt.bencoding.model.BEInteger;
import bt.bencoding.model.BEMap;
import bt.bencoding.model.BEObject;
import bt.bencoding.model.BEString;
import bt.metainfo.TorrentId;
import bt.net.InetPeer;
import bt.net.Peer;
import bt.runtime.Config;
import bt.service.IdentityService;
import bt.tracker.Tracker;
import bt.tracker.TrackerRequestBuilder;
import bt.tracker.TrackerResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class HttpTracker implements Tracker {

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_RESPONSE_BYTES = 1_048_576;

    private final IdentityService identityService;
    private final Config config;
    private final CloseableHttpClient httpClient;
    private final ScheduledExecutorService timeoutScheduler;
    private final URI trackerUri;

    HttpTracker(IdentityService identityService, Config config, CloseableHttpClient httpClient,
                ScheduledExecutorService timeoutScheduler, String trackerUrl) {
        this.identityService = identityService;
        this.config = config;
        this.httpClient = httpClient;
        this.timeoutScheduler = timeoutScheduler;
        this.trackerUri = URI.create(trackerUrl);
        validateUriStructure(trackerUri);
    }

    @Override
    public TrackerRequestBuilder request(TorrentId torrentId) {
        return new TrackerRequestBuilder(torrentId) {
            @Override
            public TrackerResponse start() {
                return announce("started");
            }

            @Override
            public TrackerResponse stop() {
                return announce("stopped");
            }

            @Override
            public TrackerResponse complete() {
                return announce("completed");
            }

            @Override
            public TrackerResponse query() {
                return announce(null);
            }

            private TrackerResponse announce(String event) {
                try {
                    URI uri = buildUri(this, event);
                    return send(uri, 0);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return TrackerResponse.exceptional(exception);
                } catch (Exception exception) {
                    return TrackerResponse.exceptional(exception);
                }
            }
        };
    }

    private TrackerResponse send(URI uri, int redirects) throws IOException, InterruptedException {
        validateUriStructure(uri);
        HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", "text/plain, application/octet-stream");
        long timeoutMillis = config.getPeerConnectionTimeout().toMillis();
        int socketTimeoutMillis = (int) Math.min(Integer.MAX_VALUE, Math.max(1, timeoutMillis));
        request.setConfig(RequestConfig.custom()
                .setConnectTimeout(socketTimeoutMillis)
                .setConnectionRequestTimeout(socketTimeoutMillis)
                .setSocketTimeout(socketTimeoutMillis)
                .build());
        ScheduledFuture<?> timeout = timeoutScheduler.schedule(
                request::abort, timeoutMillis, TimeUnit.MILLISECONDS);
        URI redirectUri = null;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (isRedirect(statusCode)) {
                if (redirects >= MAX_REDIRECTS) {
                    return TrackerResponse.failure("Tracker returned too many redirects");
                }
                Header location = response.getFirstHeader("Location");
                if (location == null) {
                    return TrackerResponse.failure("Tracker redirect omitted Location");
                }
                redirectUri = uri.resolve(location.getValue());
                if ("https".equalsIgnoreCase(uri.getScheme())
                        && !"https".equalsIgnoreCase(redirectUri.getScheme())) {
                    return TrackerResponse.failure("Tracker redirect attempted to downgrade HTTPS");
                }
            } else if (statusCode < 200 || statusCode >= 300) {
                return TrackerResponse.failure("Tracker returned HTTP " + statusCode);
            } else {
                if (entity == null) {
                    return TrackerResponse.failure("Tracker returned an empty response");
                }
                long contentLength = entity.getContentLength();
                if (contentLength > MAX_RESPONSE_BYTES) {
                    return TrackerResponse.failure("Tracker response exceeded size limit");
                }
                try (InputStream body = entity.getContent()) {
                    byte[] responseBody = body.readNBytes(MAX_RESPONSE_BYTES + 1);
                    if (responseBody.length > MAX_RESPONSE_BYTES) {
                        return TrackerResponse.failure("Tracker response exceeded size limit");
                    }
                    return parseResponse(responseBody);
                }
            }
        } finally {
            timeout.cancel(false);
        }
        return send(redirectUri, redirects + 1);
    }

    private static void validateUriStructure(URI uri) {
        String scheme = uri.getScheme();
        if ((!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Invalid HTTP tracker URL");
        }
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303
                || statusCode == 307 || statusCode == 308;
    }

    private URI buildUri(TrackerRequestBuilder request, String event) {
        StringBuilder query = new StringBuilder();
        append(query, "info_hash", percentEncode(request.getTorrentId().getBytes()));
        append(query, "peer_id", percentEncode(identityService.getLocalPeerId().getBytes()));
        append(query, "port", Integer.toString(config.getAcceptorPort()));
        append(query, "uploaded", Long.toString(request.getUploaded()));
        append(query, "downloaded", Long.toString(request.getDownloaded()));
        append(query, "left", Long.toString(request.getLeft()));
        append(query, "compact", "1");
        append(query, "numwant", Integer.toString(config.getNumberOfPeersToRequestFromTracker()));
        if (event != null) {
            append(query, "event", event);
        }
        String existingQuery = trackerUri.getRawQuery();
        String combinedQuery = existingQuery == null || existingQuery.isEmpty()
                ? query.toString()
                : existingQuery + "&" + query;
        String base = trackerUri.toASCIIString();
        int fragmentStart = base.indexOf('#');
        if (fragmentStart >= 0) {
            base = base.substring(0, fragmentStart);
        }
        int queryStart = base.indexOf('?');
        if (queryStart >= 0) {
            base = base.substring(0, queryStart);
        }
        return URI.create(base + "?" + combinedQuery);
    }

    private TrackerResponse parseResponse(byte[] body) {
        if (body.length == 0) {
            return TrackerResponse.failure("Tracker returned an empty response");
        }
        try (BEParser parser = new BEParser(body)) {
            BEMap root = parser.readMap();
            Map<String, BEObject<?>> values = root.getValue();
            String failure = string(values.get("failure reason"));
            if (failure != null) {
                return TrackerResponse.failure(failure);
            }

            TrackerResponse response = TrackerResponse.ok();
            response.setWarningMessage(string(values.get("warning message")));
            response.setInterval(integer(values.get("interval")));
            response.setMinInterval(integer(values.get("min interval")));
            response.setSeederCount(integer(values.get("complete")));
            response.setLeecherCount(integer(values.get("incomplete")));
            byte[] trackerId = bytes(values.get("tracker id"));
            if (trackerId != null) {
                response.setTrackerId(trackerId);
            }
            byte[] compactPeers = bytes(values.get("peers"));
            if (compactPeers != null) {
                response.setPeers(parseCompactPeers(compactPeers));
            }
            return response;
        } catch (Exception exception) {
            return TrackerResponse.exceptional(new BtException("Invalid HTTP tracker response", exception));
        }
    }

    private List<Peer> parseCompactPeers(byte[] peers) throws Exception {
        if (peers.length % 6 != 0) {
            throw new BtException("Invalid compact peer list length: " + peers.length);
        }
        List<Peer> result = new ArrayList<>(peers.length / 6);
        for (int offset = 0; offset < peers.length; offset += 6) {
            byte[] address = new byte[]{peers[offset], peers[offset + 1], peers[offset + 2], peers[offset + 3]};
            int port = ((peers[offset + 4] & 0xff) << 8) | (peers[offset + 5] & 0xff);
            result.add(InetPeer.builder(InetAddress.getByAddress(address), port).build());
        }
        return result;
    }

    private static String string(BEObject<?> value) {
        byte[] bytes = bytes(value);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] bytes(BEObject<?> value) {
        return value instanceof BEString string ? string.getValue() : null;
    }

    private static int integer(BEObject<?> value) {
        return value instanceof BEInteger integer ? integer.getValue().intValue() : 0;
    }

    private static void append(StringBuilder query, String name, String value) {
        if (!query.isEmpty()) {
            query.append('&');
        }
        query.append(name).append('=').append(value);
    }

    private static String percentEncode(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 3);
        for (byte current : value) {
            result.append('%');
            int unsigned = current & 0xff;
            result.append(Character.toUpperCase(Character.forDigit(unsigned >>> 4, 16)));
            result.append(Character.toUpperCase(Character.forDigit(unsigned & 0x0f, 16)));
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "HttpTracker{" + "trackerUri=" + trackerUri + '}';
    }
}
