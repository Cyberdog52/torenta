package ch.andreskonrad.torenta.bittorrent.service;

import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Determines the local address that actually carries internet traffic.
 * The vendored BitTorrent engine defaults to the first non-loopback IPv4 address of any network
 * interface, which on machines with Hyper-V, WSL, VPN or unconfigured adapters is a virtual or
 * link-local address without a route to the internet. Binding to such an address makes every
 * tracker announce fail with "Network is unreachable", leaving downloads without peers.
 */
final class RoutableAddressResolver {

    private static final String ROUTE_PROBE_HOST = "1.1.1.1";
    private static final int ROUTE_PROBE_PORT = 53;

    private RoutableAddressResolver() {
    }

    static Optional<InetAddress> resolve() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress(InetAddress.getByName(ROUTE_PROBE_HOST), ROUTE_PROBE_PORT));
            InetAddress localAddress = socket.getLocalAddress();
            return isUsable(localAddress) ? Optional.of(localAddress) : Optional.empty();
        } catch (SocketException | UnknownHostException | UncheckedIOException exception) {
            return Optional.empty();
        }
    }

    private static boolean isUsable(InetAddress address) {
        return address instanceof Inet4Address
                && !address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress();
    }
}
