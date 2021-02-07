package ch.andreskonrad.torenta.torrent.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.torrent.api.PirateBayHtmlAPI;
import ch.andreskonrad.torenta.torrent.api.TorrentParseAPI;
import ch.andreskonrad.torenta.torrent.api.TorrentQuery;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames={CustomCacheConfig.PIRATE_BAY_CACHE_NAME})
public class TorrentService {

    private final static boolean usePirateBayHtmlParsing = true;

    @Cacheable
    public List<TorrentEntry> search(String searchString) throws TorrentSearchException {
        TorrentQuery query = new TorrentQuery(searchString);

        if (usePirateBayHtmlParsing) {
            return PirateBayHtmlAPI.search(query);
        } else {
            return TorrentParseAPI.search(query);
        }

    }
}
