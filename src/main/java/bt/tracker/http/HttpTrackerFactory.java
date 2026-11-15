package bt.tracker.http;

import bt.CountingThreadFactory;
import bt.runtime.Config;
import bt.service.IdentityService;
import bt.service.IRuntimeLifecycleBinder;
import bt.tracker.Tracker;
import bt.tracker.TrackerFactory;
import com.google.inject.Inject;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

public class HttpTrackerFactory implements TrackerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpTrackerFactory.class);

    private final IdentityService identityService;
    private final Config config;
    private final CloseableHttpClient httpClient;
    private final ScheduledExecutorService timeoutScheduler;

    @Inject
    public HttpTrackerFactory(IdentityService identityService, Config config,
                              IRuntimeLifecycleBinder lifecycleBinder) {
        this.identityService = identityService;
        this.config = config;
        this.httpClient = createHttpClient(HttpTrackerFactory::isPublicTrackerAddress);
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor(
                CountingThreadFactory.daemonFactory("bt.tracker.http.timeout"));
        lifecycleBinder.onShutdown("Shutdown HTTP tracker client", this::shutdown);
    }

    @Override
    public Tracker getTracker(String trackerUrl) {
        return new HttpTracker(identityService, config, httpClient, timeoutScheduler, trackerUrl);
    }

    static CloseableHttpClient createHttpClient(Predicate<InetAddress> addressPolicy) {
        DnsResolver resolver = host -> {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (!addressPolicy.test(address)) {
                    throw new UnknownHostException("HTTP tracker resolved to a non-public address");
                }
            }
            return addresses;
        };
        Registry<ConnectionSocketFactory> socketFactories = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(socketFactories, resolver);
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableRedirectHandling()
                .build();
    }

    static boolean isPublicTrackerAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            int third = bytes[2] & 0xff;
            return first != 0
                    && first != 10
                    && first != 127
                    && first < 224
                    && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 169 && second == 254)
                    && !(first == 172 && second >= 16 && second <= 31)
                    && !(first == 192 && second == 0 && (third == 0 || third == 2))
                    && !(first == 192 && second == 88 && third == 99)
                    && !(first == 192 && second == 168)
                    && !(first == 198 && (second == 18 || second == 19))
                    && !(first == 198 && second == 51 && third == 100)
                    && !(first == 203 && second == 0 && third == 113);
        }
        boolean globalUnicast = (bytes[0] & 0xe0) == 0x20;
        boolean protocolAssignments = bytes[0] == 0x20 && bytes[1] == 0x01
                && (bytes[2] & 0xff) <= 1;
        boolean documentation = bytes[0] == 0x20 && bytes[1] == 0x01
                && bytes[2] == 0x0d && (bytes[3] & 0xff) == 0xb8;
        boolean sixToFour = bytes[0] == 0x20 && bytes[1] == 0x02;
        boolean documentationV2 = (bytes[0] & 0xff) == 0x3f && (bytes[1] & 0xff) == 0xff
                && (bytes[2] & 0xf0) == 0;
        return globalUnicast && !protocolAssignments && !documentation && !sixToFour && !documentationV2;
    }

    private void shutdown() {
        timeoutScheduler.shutdownNow();
        try {
            httpClient.close();
        } catch (IOException exception) {
            LOGGER.warn("Failed to close HTTP tracker client", exception);
        }
    }
}
