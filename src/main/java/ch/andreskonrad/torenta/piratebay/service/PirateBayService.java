package ch.andreskonrad.torenta.piratebay.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.piratebay.api.PirateBayAPI;
import ch.andreskonrad.torenta.piratebay.api.PirateBayCategory;
import ch.andreskonrad.torenta.piratebay.api.PirateBayQuery;
import ch.andreskonrad.torenta.piratebay.api.PirateBayQueryOrder;
import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntry;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@CacheConfig(cacheNames={CustomCacheConfig.PIRATE_BAY_CACHE_NAME})
public class PirateBayService {

    @Cacheable
    public ArrayList<PirateBayEntry> search(String searchString) throws PirateBaySearchException {
        PirateBayQuery query = new PirateBayQuery(searchString, 0, PirateBayCategory.All, PirateBayQueryOrder.ByDefault);

        return PirateBayAPI.search(query);
    }
}
