package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorrentParseAPITest {

    @Test
    void searchParsesValidJsonAndUsesBackendUri() throws Exception {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TorrentParseAPI api = new TorrentParseAPI(uri -> {
            requestedUri.set(uri);
            return """
                    [{
                      "id": 9,
                      "name": "Film & More",
                      "info_hash": "ABC",
                      "leechers": 4,
                      "seeders": 8,
                      "num_files": 1,
                      "size": 1024,
                      "username": "alice",
                      "added": 0,
                      "status": "vip",
                      "category": 200,
                      "imdb": "tt1"
                    }]
                    """;
        });

        List<TorrentEntry> entries = api.find(new TorrentQuery("Film & More"));

        assertEquals(URI.create("https://apibay.org?q=Film+%26+More&cat="), requestedUri.get());
        assertEquals(1, entries.size());
        TorrentEntry entry = entries.getFirst();
        assertEquals("Film & More", entry.getName());
        assertEquals(8, entry.getNumberOfSeeders());
        assertEquals(4, entry.getNumberOfLeechers());
        assertEquals("alice", entry.getUploader());
        assertEquals(" 1.00 KB", entry.getSize());
        assertTrue(entry.isUploaderIsVIP());
    }

    @Test
    void searchReturnsEmptyForEmptyArrayAndEmptyOrMalformedResponses() throws Exception {
        assertTrue(new TorrentParseAPI(uri -> "[]").find(new TorrentQuery("empty")).isEmpty());
        assertTrue(new TorrentParseAPI(uri -> "").find(new TorrentQuery("empty")).isEmpty());
        assertTrue(new TorrentParseAPI(uri -> "{bad json").find(new TorrentQuery("bad")).isEmpty());
        assertTrue(new TorrentParseAPI(uri -> null).find(new TorrentQuery("null")).isEmpty());
    }

    @Test
    void searchReturnsEmptyForTransportFailure() throws Exception {
        TorrentParseAPI api = new TorrentParseAPI(uri -> {
            throw new IOException("offline");
        });

        assertTrue(api.find(new TorrentQuery("offline")).isEmpty());
    }

    @Test
    void searchRestoresInterruptAndReturnsEmpty() throws Exception {
        TorrentParseAPI api = new TorrentParseAPI(uri -> {
            throw new InterruptedException("stopped");
        });

        assertTrue(api.find(new TorrentQuery("interrupted")).isEmpty());
        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(Thread.interrupted());
        assertFalse(Thread.currentThread().isInterrupted());
    }
}
