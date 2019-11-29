package bt.bencoding;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.Config;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BtClientTest {

    @Test
    public void manual_btClient_downloadToUserDir() {
        Config config = new Config() {
            @Override
            public int getNumOfHashingThreads() {
                return Runtime.getRuntime().availableProcessors() * 2;
            }
        };

        Path targetDirectory = Paths.get(System.getProperty("user.home"), "Downloads");

        Storage storage = new FileSystemStorage(targetDirectory);

        BtClient client = Bt.client()
                .config(config)
                .storage(storage)
                .magnet("magnet:?xt=urn:btih:f415b7ebae1df2cea2a4c21a4a42fe3ff7dfed51&dn=The.Walking.Dead.S09E11.WEB.h264-TBS%5Bettv%5D&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Fopen.demonii.com%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Fexodus.desync.com%3A6969")
                .autoLoadModules()
                .stopWhenDownloaded()
                .build();

        client.startAsync().join();
    }
}
