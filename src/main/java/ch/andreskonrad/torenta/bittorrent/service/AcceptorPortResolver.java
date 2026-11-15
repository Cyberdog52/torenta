package ch.andreskonrad.torenta.bittorrent.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

final class AcceptorPortResolver {

    private AcceptorPortResolver() {
    }

    static ServerSocketChannel reserve(InetAddress address, int preferredPort) {
        try {
            return probe(address, preferredPort);
        } catch (BindException exception) {
            try {
                return probe(address, 0);
            } catch (IOException fallbackException) {
                fallbackException.addSuppressed(exception);
                throw new UncheckedIOException("Failed to find an available BitTorrent acceptor port",
                        fallbackException);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to check the preferred BitTorrent acceptor port", exception);
        }
    }

    private static ServerSocketChannel probe(InetAddress address, int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        try {
            channel.bind(new InetSocketAddress(address, port));
            return channel;
        } catch (IOException exception) {
            channel.close();
            throw exception;
        }
    }
}
