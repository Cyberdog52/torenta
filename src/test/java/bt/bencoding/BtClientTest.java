package bt.bencoding;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.Config;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BtClientTest {

    @Test
    @Disabled("Manual test, requires a valid magnet link")
    public void manual_btClient_downloadToUserDir() {

        Config config = new Config() {
            @Override
            public int getNumOfHashingThreads() {
                return Runtime.getRuntime().availableProcessors() * 2;
            }
        };

        Path targetDirectory = Paths.get(System.getProperty("user.home"), "Downloads");

        Storage storage = new FileSystemStorage(targetDirectory);

        String link = "";
        BtClient client = Bt.client()
                .config(config)
                .storage(storage)
                .magnet(link)
                .autoLoadModules()
                .stopWhenDownloaded()
                .build();

        client.startAsync().join();
    }

}
