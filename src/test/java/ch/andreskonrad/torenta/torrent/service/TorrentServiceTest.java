package ch.andreskonrad.torenta.torrent.service;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TorrentService.class)
@EnableConfigurationProperties
public class TorrentServiceTest {

    @Autowired
    private TorrentService torrentService;

    @Ignore
    @Test
    public void manualSearch() throws TorrentSearchException {
        ArrayList<TorrentEntry> entries = torrentService.search("Walking Dead");

        assertTrue(entries.size() > 0);
    }

}
