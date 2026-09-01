package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import ch.andreskonrad.torenta.tmdb.dto.*;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Locale;

@Service
@CacheConfig(cacheNames={CustomCacheConfig.TMDB_CACHE_NAME})
public class TmdbService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbService.class);
    private static final String HOST = "api.themoviedb.org";
    private static final String SCHEME = "https";

    private final PreferenceService preferenceService;
    private final TmdbHttpTransport httpTransport;
    private final ObjectMapper objectMapper;

    @Autowired
    public TmdbService(
            PreferenceService preferenceService,
            TmdbHttpTransport httpTransport,
            ObjectMapper objectMapper
    ) {
        this.preferenceService = preferenceService;
        this.httpTransport = httpTransport;
        this.objectMapper = objectMapper;
    }

    @Cacheable
    public TmdbSeriesSearchResultDto searchSeries(String searchString) {
        return request(searchSeriesUri(searchString), TmdbSeriesSearchResultDto.class);
    }

    @Cacheable
    public TmdbMoviesSearchResultDto searchMovies(String searchString) {
        return request(searchMovieUri(searchString), TmdbMoviesSearchResultDto.class);
    }

    @Cacheable
    public TmdbSeriesDetailDto getSeries(int id) {
        return request(detailSeriesUri(id), TmdbSeriesDetailDto.class);
    }

    @Cacheable
    public TmdbMovieDetailDto getMovie(int id) {
        return request(detailMovieUri(id), TmdbMovieDetailDto.class);
    }

    @Cacheable
    public TmdbEpisodeDto[] getEpisodes(int seriesId, int season_number) {
        TmdbSeasonDto season = request(seasonUri(seriesId, season_number), TmdbSeasonDto.class);
        return season == null || season.getEpisodes() == null
                ? new TmdbEpisodeDto[0]
                : season.getEpisodes();
    }

    private URI detailSeriesUri(int id) {
        return getDefaultComponentsBuilder()
                .path("/3/tv/" + id)
                .build()
                .encode()
                .toUri();
    }

    private URI detailMovieUri(int id) {
        return getDefaultComponentsBuilder()
                .path("/3/movie/" + id)
                .build()
                .encode()
                .toUri();
    }

    private URI searchSeriesUri(String searchString) {
        return getDefaultComponentsBuilder()
                .path("/3/search/tv")
                .queryParam("query", searchString.toLowerCase(Locale.ROOT))
                .queryParam("page", "1")
                .build()
                .encode()
                .toUri();
    }

    private URI searchMovieUri(String searchString) {
        return getDefaultComponentsBuilder()
                .path("/3/search/movie")
                .queryParam("query", searchString.toLowerCase(Locale.ROOT))
                .queryParam("page", "1")
                .queryParam("include_adult", false)
                .build()
                .encode()
                .toUri();
    }

    private UriComponentsBuilder getDefaultComponentsBuilder() {
        return UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(HOST)
                .queryParam("api_key", preferenceService.loadPreferences().getTmdbServiceKey())
                .queryParam("language", "en-US");
    }

    private <T> T request(URI uri, Class<T> responseType) {
        try {
            return objectMapper.readValue(httpTransport.get(uri), responseType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("TMDB request was interrupted");
        } catch (Exception e) {
            LOGGER.warn("TMDB request or response processing failed");
        }
        return null;
    }

    private URI seasonUri(int seriesId, int season_number) {
        return getDefaultComponentsBuilder()
                .path("/3/tv/" + seriesId + "/season/" + season_number)
                .build()
                .encode()
                .toUri();
    }
}
