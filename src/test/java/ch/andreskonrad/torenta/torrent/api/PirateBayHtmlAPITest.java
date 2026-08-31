package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PirateBayHtmlAPITest {

    @Test
    void searchParsesRowsAndSkipsHeadersAndMalformedRows() {
        Document document = Jsoup.parse("""
                <table>
                  <tr class="header">
                    <td><a>Ignored</a></td><td><a>Header</a></td><td>x</td>
                    <td><a href="ignored"></a></td><td>x</td><td>1</td><td>2</td><td>x</td>
                  </tr>
                  <tr>
                    <td><a>Video</a></td>
                    <td><a>Rock &amp; Röll</a></td>
                    <td>Today 10:30</td>
                    <td>
                      <a href="magnet:?xt=urn:btih:ABC"></a>
                      <img alt="VIP"><span><img alt="Trusted"></span>
                    </td>
                    <td>1.00 GiB</td><td>123</td><td>45</td><td>alice</td>
                  </tr>
                  <tr><td>malformed</td></tr>
                </table>
                """);
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        PirateBayHtmlAPI api = new PirateBayHtmlAPI(uri -> {
            requestedUri.set(uri);
            return document;
        });

        List<TorrentEntry> entries = api.find(new TorrentQuery("Rock & Röll"));

        assertEquals(URI.create("https://tpb.party/search/Rock%20&%20R%C3%B6ll"), requestedUri.get());
        assertEquals(1, entries.size());
        TorrentEntry entry = entries.getFirst();
        assertEquals("Video", entry.getCategory());
        assertEquals("Rock & Röll", entry.getName());
        assertEquals("Today 10:30", entry.getUploadedTime());
        assertEquals("magnet:?xt=urn:btih:ABC", entry.getMagnetLink());
        assertEquals("1.00 GiB", entry.getSize());
        assertEquals(123, entry.getNumberOfSeeders());
        assertEquals(45, entry.getNumberOfLeechers());
        assertEquals("alice", entry.getUploader());
        assertTrue(entry.isUploaderIsVIP());
        assertTrue(entry.isUploaderIsTrusted());
    }

    @Test
    void searchLeavesUploaderFlagsFalseWithoutIcons() {
        Document document = Jsoup.parse("""
                <table><tr>
                  <td><a>Audio</a></td><td><a>Name</a></td><td>Now</td>
                  <td><a href="magnet:test"></a></td>
                  <td>10 B</td><td>1</td><td>0</td><td>bob</td>
                </tr></table>
                """);

        TorrentEntry entry = new PirateBayHtmlAPI(uri -> document)
                .find(new TorrentQuery("Name"))
                .getFirst();

        assertFalse(entry.isUploaderIsVIP());
        assertFalse(entry.isUploaderIsTrusted());
    }

    @Test
    void searchRetriesUntilTransportSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        PirateBayHtmlAPI api = new PirateBayHtmlAPI(uri -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IOException("temporary");
            }
            return Jsoup.parse("<table></table>");
        });

        assertTrue(api.find(new TorrentQuery("retry")).isEmpty());
        assertEquals(3, attempts.get());
    }

    @Test
    void searchThrowsBadGatewayAfterRetryExhaustion() {
        AtomicInteger attempts = new AtomicInteger();
        PirateBayHtmlAPI api = new PirateBayHtmlAPI(uri -> {
            attempts.incrementAndGet();
            throw new IOException("offline");
        });

        HttpServerErrorException exception = assertThrows(
                HttpServerErrorException.class,
                () -> api.find(new TorrentQuery("offline")));

        assertEquals(502, exception.getStatusCode().value());
        assertEquals(4, attempts.get());
    }
}
