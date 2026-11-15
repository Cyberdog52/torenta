package ch.andreskonrad.torenta.bittorrent.service;

import bt.Bt;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.BtRuntime;
import bt.runtime.Config;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

@Component
public class DefaultBitTorrentClientFactory implements BitTorrentClientFactory {

    private final Function<Config, BtRuntime> runtimeFactory;
    private volatile BtRuntime runtime;

    public DefaultBitTorrentClientFactory() {
        this(config -> BtRuntime.builder(config)
                .autoLoadModules()
                .disableAutomaticShutdown()
                .build());
    }

    DefaultBitTorrentClientFactory(Function<Config, BtRuntime> runtimeFactory) {
        this.runtimeFactory = runtimeFactory;
    }

    @Override
    public BtClient create(Config config, Path targetDirectory, String magnetLink) {
        return Bt.client(getOrCreateRuntime(config))
                .storage(new FileSystemStorage(targetDirectory))
                .magnet(magnetLink)
                .stopWhenDownloaded()
                .build();
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
            }
            runtime = runtimeFactory.apply(config);
            return runtime;
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
            if (field.isSynthetic()) {
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
            runtime = null;
        }
    }
}
