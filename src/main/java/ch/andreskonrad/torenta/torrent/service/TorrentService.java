package ch.andreskonrad.torenta.torrent.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.torrent.api.TorrentParseAPI;
import ch.andreskonrad.torenta.torrent.api.TorrentQuery;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@CacheConfig(cacheNames={CustomCacheConfig.PIRATE_BAY_CACHE_NAME})
public class TorrentService {

    @Cacheable
    public ArrayList<TorrentEntry> search(String searchString) throws TorrentSearchException {
        TorrentQuery query = new TorrentQuery(searchString);

        return TorrentParseAPI.search(query);
    }
}
