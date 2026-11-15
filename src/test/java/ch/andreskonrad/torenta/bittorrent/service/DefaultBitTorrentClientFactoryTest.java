package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtRuntime;
import bt.runtime.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultBitTorrentClientFactoryTest {

    @TempDir
    Path root;

    @Test
    void create_reusesRuntimeForEquivalentConfig() {
        BtRuntime runtime = mock(BtRuntime.class);
        AtomicInteger creationCount = new AtomicInteger();
        AtomicReference<Config> runtimeConfig = new AtomicReference<>();
        Config firstConfig = new Config();
        DefaultBitTorrentClientFactory factory = new DefaultBitTorrentClientFactory(config -> {
            creationCount.incrementAndGet();
            runtimeConfig.set(config);
            when(runtime.getConfig()).thenReturn(config);
            return runtime;
        });

        factory.create(firstConfig, root.resolve("first"),
                "magnet:?xt=urn:btih:1111111111111111111111111111111111111111");
        factory.create(new Config(firstConfig), root.resolve("second"),
                "magnet:?xt=urn:btih:2222222222222222222222222222222222222222");

        assertEquals(1, creationCount.get());
        assertSame(firstConfig, runtimeConfig.get());
    }

    @Test
    void create_recreatesRuntimeWhenConfigDiffers() {
        BtRuntime firstRuntime = mock(BtRuntime.class);
        BtRuntime secondRuntime = mock(BtRuntime.class);
        AtomicInteger creationCount = new AtomicInteger();
        AtomicReference<Config> lastRuntimeConfig = new AtomicReference<>();
        Config firstConfig = new Config();
        Config secondConfig = new Config();
        DefaultBitTorrentClientFactory factory = new DefaultBitTorrentClientFactory(config -> {
            creationCount.incrementAndGet();
            lastRuntimeConfig.set(config);
            if (creationCount.get() == 1) {
                when(firstRuntime.getConfig()).thenReturn(firstConfig);
                return firstRuntime;
            }
            when(secondRuntime.getConfig()).thenReturn(secondConfig);
            return secondRuntime;
        });
        secondConfig.setAcceptorPort(secondConfig.getAcceptorPort() + 1);

        factory.create(firstConfig, root.resolve("first"),
                "magnet:?xt=urn:btih:1111111111111111111111111111111111111111");
        factory.create(secondConfig, root.resolve("second"),
                "magnet:?xt=urn:btih:2222222222222222222222222222222222222222");

        assertEquals(2, creationCount.get());
        assertSame(secondConfig, lastRuntimeConfig.get());
        verify(firstRuntime).shutdown();
    }

    @Test
    void shutdown_stopsSharedRuntimeOnlyOnce() {
        BtRuntime runtime = mock(BtRuntime.class);
        DefaultBitTorrentClientFactory factory = new DefaultBitTorrentClientFactory(config -> runtime);
        factory.create(new Config(), root,
                "magnet:?xt=urn:btih:1111111111111111111111111111111111111111");

        factory.shutdown();
        factory.shutdown();

        verify(runtime).shutdown();
    }
}
