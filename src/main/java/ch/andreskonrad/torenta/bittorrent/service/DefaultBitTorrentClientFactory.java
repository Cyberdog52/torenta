package ch.andreskonrad.torenta.bittorrent.service;

import bt.Bt;
import bt.data.file.FileSystemStorage;
import bt.data.file.FileSystemResumeData;
import bt.runtime.BtClient;
import bt.runtime.BtRuntime;
import bt.runtime.Config;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadResumeCheckpoint;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

@Component
public class DefaultBitTorrentClientFactory implements BitTorrentClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBitTorrentClientFactory.class);

    private final Function<Config, BtRuntime> runtimeFactory;
    private final boolean reserveAcceptorPort;
    private volatile BtRuntime runtime;
    private volatile Config runtimeConfig;

    public DefaultBitTorrentClientFactory() {
        this(config -> BtRuntime.builder(config)
                .autoLoadModules()
                .disableAutomaticShutdown()
                .build(), true);
    }

    DefaultBitTorrentClientFactory(Function<Config, BtRuntime> runtimeFactory) {
        this(runtimeFactory, false);
    }

    DefaultBitTorrentClientFactory(Function<Config, BtRuntime> runtimeFactory, boolean reserveAcceptorPort) {
        this.runtimeFactory = runtimeFactory;
        this.reserveAcceptorPort = reserveAcceptorPort;
    }

    @Override
    public BtClient create(Config config, Path targetDirectory, String magnetLink,
                           DownloadResumeCheckpoint resumeCheckpoint) {
        return Bt.client(getOrCreateRuntime(config))
                .storage(new FileSystemStorage(targetDirectory, toResumeData(resumeCheckpoint)))
                .magnet(magnetLink)
                .stopWhenDownloaded()
                .build();
    }

    private FileSystemResumeData toResumeData(DownloadResumeCheckpoint checkpoint) {
        if (checkpoint == null || checkpoint.verifiedPieces() == null || checkpoint.files() == null) {
            return null;
        }
        return new FileSystemResumeData(
                checkpoint.torrentId(),
                checkpoint.piecesTotal(),
                checkpoint.verifiedPieces(),
                checkpoint.files().stream()
                        .map(file -> new FileSystemResumeData.FileSnapshot(
                                file.relativePath(), file.size(), file.lastModifiedNanos(), file.fileKey()))
                        .toList());
    }

    private BtRuntime getOrCreateRuntime(Config config) {
        BtRuntime currentRuntime = runtime;
        if (currentRuntime != null && sameConfig(currentRuntime.getConfig(), config)) {
            return currentRuntime;
        }
        synchronized (this) {
            currentRuntime = runtime;
            if (currentRuntime != null && sameConfig(currentRuntime.getConfig(), config)) {
                return currentRuntime;
            }
            if (currentRuntime != null) {
                currentRuntime.shutdown();
                closeReservedChannel(runtimeConfig);
                runtime = null;
                runtimeConfig = null;
            }
            if (reserveAcceptorPort) {
                reserveAcceptorPort(config);
            }
            try {
                runtime = runtimeFactory.apply(config);
                runtimeConfig = config;
            } catch (RuntimeException | Error exception) {
                closeReservedChannel(config);
                throw exception;
            }
            return runtime;
        }
    }

    private void reserveAcceptorPort(Config config) {
        ServerSocketChannel existing = config.getAcceptorServerChannel();
        if (existing != null && existing.isOpen()) {
            return;
        }
        int preferredPort = config.getAcceptorPort();
        ServerSocketChannel reservation = AcceptorPortResolver.reserve(config.getAcceptorAddress(), preferredPort);
        try {
            int actualPort = ((InetSocketAddress) reservation.getLocalAddress()).getPort();
            config.setAcceptorPort(actualPort);
            config.setAcceptorServerChannel(reservation);
            if (actualPort != preferredPort) {
                LOGGER.warn("BitTorrent acceptor port {} is unavailable; using port {} instead",
                        preferredPort, actualPort);
            }
        } catch (IOException exception) {
            closeChannel(reservation);
            throw new UncheckedIOException("Failed to inspect reserved BitTorrent acceptor port", exception);
        }
    }

    private boolean sameConfig(Config left, Config right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        for (Field field : Config.class.getDeclaredFields()) {
            if (field.isSynthetic() || field.getName().equals("acceptorServerChannel")) {
                continue;
            }
            field.setAccessible(true);
            try {
                if (!Objects.equals(field.get(left), field.get(right))) {
                    return false;
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to compare runtime config fields", exception);
            }
        }
        return true;
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (runtime != null) {
            runtime.shutdown();
            closeReservedChannel(runtimeConfig);
            runtime = null;
            runtimeConfig = null;
        }
    }

    private void closeReservedChannel(Config config) {
        if (config != null) {
            closeChannel(config.getAcceptorServerChannel());
            config.setAcceptorServerChannel(null);
        }
    }

    private void closeChannel(ServerSocketChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException exception) {
                LOGGER.warn("Failed to close reserved BitTorrent acceptor port", exception);
            }
        }
    }
}
