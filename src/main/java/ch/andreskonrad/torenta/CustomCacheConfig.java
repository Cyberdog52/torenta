package ch.andreskonrad.torenta;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static javax.management.timer.Timer.ONE_HOUR;
import static javax.management.timer.Timer.ONE_MINUTE;

@Configuration
@EnableScheduling
@EnableCaching
public class CustomCacheConfig {

    public static final String TMDB_CACHE_NAME = "tmdbSearch";
    public static final String PIRATE_BAY_CACHE_NAME = "torrentSearch";
    public static final String RECOMMENDATION_CACHE_NAME = "recommendation";

    //fully clear cache every hour
    @Scheduled(fixedRate = ONE_HOUR)
    @CacheEvict(value = { TMDB_CACHE_NAME, PIRATE_BAY_CACHE_NAME }, allEntries = true)
    public void clearCache() {
    }

    //recommendations are recomputed from the filesystem + TMDB, keep them fresh with a shorter TTL
    @Scheduled(fixedRate = 5 * ONE_MINUTE)
    @CacheEvict(value = { RECOMMENDATION_CACHE_NAME }, allEntries = true)
    public void clearRecommendationCache() {
    }

}
