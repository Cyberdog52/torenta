package ch.andreskonrad.torenta.torrent.service;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TorrentService.class)
@EnableConfigurationProperties
public class TorrentServiceTest {

    @Autowired
    private TorrentService torrentService;

    @Disabled
    @Test
    public void manualSearch() throws TorrentSearchException {
        List<TorrentEntry> entries = torrentService.search("Walking Dead");

        assertTrue(entries.size() > 0);
    }

}
