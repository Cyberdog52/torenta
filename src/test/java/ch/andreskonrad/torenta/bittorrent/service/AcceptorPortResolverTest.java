package ch.andreskonrad.torenta.bittorrent.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcceptorPortResolverTest {

    private final InetAddress loopback = InetAddress.getLoopbackAddress();

    @Test
    void resolve_returnsPreferredPortWhenItIsAvailable() throws IOException {
        int preferredPort;
        try (ServerSocketChannel channel = bind(0)) {
            preferredPort = localPort(channel);
        }

        try (ServerSocketChannel reservation = AcceptorPortResolver.reserve(loopback, preferredPort)) {
            assertEquals(preferredPort, localPort(reservation));
        }
    }

    @Test
    void resolve_returnsAvailableFallbackWhenPreferredPortIsOccupied() throws IOException {
        try (ServerSocketChannel occupiedChannel = bind(0)) {
            int occupiedPort = localPort(occupiedChannel);

            try (ServerSocketChannel reservation = AcceptorPortResolver.reserve(loopback, occupiedPort)) {
                int resolvedPort = localPort(reservation);
                assertNotEquals(occupiedPort, resolvedPort);
                assertThrows(IOException.class, () -> bind(resolvedPort));
            }
        }
    }

    private ServerSocketChannel bind(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress(loopback, port));
        return channel;
    }

    private int localPort(ServerSocketChannel channel) throws IOException {
        return ((InetSocketAddress) channel.getLocalAddress()).getPort();
    }
}
