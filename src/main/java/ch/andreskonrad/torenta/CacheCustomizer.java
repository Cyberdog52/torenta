package ch.andreskonrad.torenta;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Component;

import static ch.andreskonrad.torenta.CustomCacheConfig.PIRATE_BAY_CACHE_NAME;
import static ch.andreskonrad.torenta.CustomCacheConfig.TMDB_CACHE_NAME;
import static java.util.Arrays.asList;


@Component
public class CacheCustomizer implements CacheManagerCustomizer<ConcurrentMapCacheManager> {

    @Override
    public void customize(ConcurrentMapCacheManager cacheManager) {
        cacheManager.setCacheNames(asList(TMDB_CACHE_NAME, PIRATE_BAY_CACHE_NAME));
    }

}
