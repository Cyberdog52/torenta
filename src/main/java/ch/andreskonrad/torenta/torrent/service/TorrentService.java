package ch.andreskonrad.torenta.torrent.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.torrent.api.TorrentProvider;
import ch.andreskonrad.torenta.torrent.api.TorrentQuery;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames={CustomCacheConfig.PIRATE_BAY_CACHE_NAME})
public class TorrentService {

    private final TorrentProvider torrentProvider;

    @Autowired
    public TorrentService(TorrentProvider torrentProvider) {
        this.torrentProvider = torrentProvider;
    }

    @Cacheable
    public List<TorrentEntry> search(String searchString) throws TorrentSearchException {
        TorrentQuery query = new TorrentQuery(searchString);
        return torrentProvider.find(query);
    }
}
