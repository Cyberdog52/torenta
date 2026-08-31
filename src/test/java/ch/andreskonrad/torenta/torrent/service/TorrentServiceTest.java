package ch.andreskonrad.torenta.torrent.service;

import ch.andreskonrad.torenta.torrent.api.PirateBayHtmlAPI;
import ch.andreskonrad.torenta.torrent.api.TorrentParseAPI;
import ch.andreskonrad.torenta.torrent.api.TorrentProvider;
import ch.andreskonrad.torenta.torrent.api.TorrentQuery;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorrentServiceTest {

    @Test
    void searchDelegatesConstructedQueryToProvider() throws Exception {
        TorrentEntry expected = new TorrentEntry();
        AtomicReference<TorrentQuery> query = new AtomicReference<>();
        TorrentService service = new TorrentService(request -> {
            query.set(request);
            return List.of(expected);
        });

        List<TorrentEntry> result = service.search("Film & More");

        assertEquals(
                "https://apibay.org?q=Film+%26+More&cat=",
                query.get().getPirateBayBackendSearchString().toASCIIString());
        assertEquals(List.of(expected), result);
    }

    @Test
    void searchPropagatesProviderFailure() {
        TorrentSearchException failure = new TorrentSearchException("failed");
        TorrentService service = new TorrentService(query -> {
            throw failure;
        });

        assertSame(failure, assertThrows(TorrentSearchException.class, () -> service.search("query")));
    }

    @Test
    void springWiringSelectsHtmlProviderByDefault() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PirateBayHtmlAPI.class, TorrentParseAPI.class, TorrentService.class);
            context.refresh();

            TorrentProvider provider = context.getBean(TorrentProvider.class);
            assertInstanceOf(PirateBayHtmlAPI.class, provider);
            assertTrue(context.containsBeanDefinition("torrentService"));
        }
    }
}
