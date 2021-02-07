package bt.bencoding;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.Config;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BtClientTest {

    @Ignore
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

        String link = "magnet:?xt=urn:btih:267BFC138BAC2DD17590E9A6DB5FC1494E9ADE7D&dn=Wonder+Woman+1984.2020.IMAX.HDRip.XviD.AC3-EVO%5BTGx%5D&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2F9.rarbg.to%3A2920%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.pirateparty.gr%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.cyberia.is%3A6969%2Fannounce";

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
