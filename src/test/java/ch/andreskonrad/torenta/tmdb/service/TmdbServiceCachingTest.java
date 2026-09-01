package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import ch.andreskonrad.torenta.concierge.dto.SearchIntent;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMoviesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterRegistry;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequest;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequestFactory;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbNameResolver;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TmdbServiceCachingTest {

    @Test
    void methodNamesNamespaceSharedCacheKeys() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(CacheTestConfiguration.class)) {
            TmdbService service = context.getBean(TmdbService.class);
            SearchIntent intent = new SearchIntent(
                    AiMediaType.ANY, List.of(), null, List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of()
            );
            TmdbDiscoverRequestFactory factory = new TmdbDiscoverRequestFactory(
                    new TmdbDiscoverFilterRegistry(), mock(TmdbNameResolver.class)
            );
            TmdbDiscoverRequest movieRequest = factory.create(intent, AiMediaType.MOVIE);
            TmdbDiscoverRequest seriesRequest = factory.create(intent, AiMediaType.SERIES);

            TmdbMoviesSearchResultDto movies = service.searchMovies("same");
            TmdbSeriesSearchResultDto series = service.searchSeries("same");
            TmdbMovieDetailDto movie = service.getMovie(1);
            TmdbSeriesDetailDto tv = service.getSeries(1);
            TmdbMoviesSearchResultDto discoveredMovies = service.discoverMovies(movieRequest);
            TmdbSeriesSearchResultDto discoveredSeries = service.discoverSeries(seriesRequest);

            assertEquals("Movie", movies.getResults().getFirst().getTitle());
            assertEquals("Series", series.getResults().getFirst().getName());
            assertEquals("Movie", movie.getTitle());
            assertEquals("Series", tv.getName());
            assertEquals("Movie", discoveredMovies.getResults().getFirst().getTitle());
            assertEquals("Series", discoveredSeries.getResults().getFirst().getName());
        }
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("tmdbSearch");
        }

        @Bean
        @Primary
        TmdbHttpTransport tmdbHttpTransport() {
            return uri -> {
                String path = uri.getPath();
                if (path.contains("/search/movie") || path.contains("/discover/movie")) {
                    return """
                            {"results":[{"id":1,"title":"Movie"}]}
                            """;
                }
                if (path.contains("/search/tv") || path.contains("/discover/tv")) {
                    return """
                            {"results":[{"id":1,"name":"Series"}]}
                            """;
                }
                if (path.equals("/3/movie/1")) {
                    return """
                            {"id":1,"title":"Movie"}
                            """;
                }
                return """
                        {"id":1,"name":"Series"}
                        """;
            };
        }

        @Bean
        TmdbService tmdbService(TmdbHttpTransport transport) {
            return new TmdbService("test-key", transport, new ObjectMapper());
        }
    }
}
