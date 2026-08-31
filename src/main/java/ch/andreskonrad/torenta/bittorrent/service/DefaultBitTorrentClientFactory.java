package ch.andreskonrad.torenta.bittorrent.service;

import bt.Bt;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.Config;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class DefaultBitTorrentClientFactory implements BitTorrentClientFactory {

    @Override
    public BtClient create(Config config, Path targetDirectory, String magnetLink) {
        return Bt.client()
                .config(config)
                .storage(new FileSystemStorage(targetDirectory))
                .magnet(magnetLink)
                .autoLoadModules()
                .stopWhenDownloaded()
                .build();
    }
}
