package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import ch.andreskonrad.torenta.tmdb.dto.*;
import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequest;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames={CustomCacheConfig.TMDB_CACHE_NAME})
public class TmdbService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbService.class);

    private final String baseUrl;
    private final Supplier<String> apiKeySupplier;
    private final TmdbHttpTransport httpTransport;
    private final ObjectMapper objectMapper;
    private final boolean interactionLoggingEnabled;

    @Autowired
    public TmdbService(
            PreferenceService preferenceService,
            @Value("${app.tmdb.base-url:https://api.themoviedb.org}") String baseUrl,
            @Value("${app.ai.logging.enabled:false}") boolean interactionLoggingEnabled,
            TmdbHttpTransport httpTransport,
            ObjectMapper objectMapper
    ) {
        this(
                () -> preferenceService.loadPreferences().getTmdbServiceKey(),
                baseUrl,
                interactionLoggingEnabled,
                httpTransport,
                objectMapper
        );
    }

    TmdbService(
            PreferenceService preferenceService,
            TmdbHttpTransport httpTransport,
            ObjectMapper objectMapper
    ) {
        this(
                () -> preferenceService.loadPreferences().getTmdbServiceKey(),
                "https://api.themoviedb.org",
                false,
                httpTransport,
                objectMapper
        );
    }

    public TmdbService(String apiKey, TmdbHttpTransport httpTransport, ObjectMapper objectMapper) {
        this(() -> apiKey, "https://api.themoviedb.org", false, httpTransport, objectMapper);
    }

    public TmdbService(
            String apiKey,
            String baseUrl,
            TmdbHttpTransport httpTransport,
            ObjectMapper objectMapper
    ) {
        this(() -> apiKey, baseUrl, false, httpTransport, objectMapper);
    }

    private TmdbService(
            Supplier<String> apiKeySupplier,
            String baseUrl,
            boolean interactionLoggingEnabled,
            TmdbHttpTransport httpTransport,
            ObjectMapper objectMapper
    ) {
        this.apiKeySupplier = apiKeySupplier;
        this.baseUrl = baseUrl;
        this.interactionLoggingEnabled = interactionLoggingEnabled;
        this.httpTransport = httpTransport;
        this.objectMapper = objectMapper;
    }

    @Cacheable(key = "'seriesSearch:' + #searchString")
    public TmdbSeriesSearchResultDto searchSeries(String searchString) {
        return request(searchSeriesUri(searchString), TmdbSeriesSearchResultDto.class);
    }

    @Cacheable(key = "'movieSearch:' + #searchString")
    public TmdbMoviesSearchResultDto searchMovies(String searchString) {
        return request(searchMovieUri(searchString), TmdbMoviesSearchResultDto.class);
    }

    @Cacheable(key = "'movieDiscover:' + #discoverRequest")
    public TmdbMoviesSearchResultDto discoverMovies(TmdbDiscoverRequest discoverRequest) {
        return request(discoverMovieUri(discoverRequest), TmdbMoviesSearchResultDto.class);
    }

    @Cacheable(key = "'seriesDiscover:' + #discoverRequest")
    public TmdbSeriesSearchResultDto discoverSeries(TmdbDiscoverRequest discoverRequest) {
        return request(discoverSeriesUri(discoverRequest), TmdbSeriesSearchResultDto.class);
    }

    @Cacheable(key = "'movieSimilar:' + #movieId")
    public TmdbMoviesSearchResultDto similarMovies(int movieId) {
        return request(similarMovieUri(movieId), TmdbMoviesSearchResultDto.class);
    }

    @Cacheable(key = "'seriesSimilar:' + #seriesId")
    public TmdbSeriesSearchResultDto similarSeries(int seriesId) {
        return request(similarSeriesUri(seriesId), TmdbSeriesSearchResultDto.class);
    }

    @Cacheable(key = "'peopleSearch:' + #searchString")
    public TmdbPersonSearchResultDto searchPeople(String searchString) {
        return request(searchPeopleUri(searchString), TmdbPersonSearchResultDto.class);
    }

    @Cacheable(key = "'companySearch:' + #searchString")
    public TmdbNamedEntitySearchResultDto searchCompanies(String searchString) {
        return request(searchNamedEntityUri("/3/search/company", searchString),
                TmdbNamedEntitySearchResultDto.class);
    }

    @Cacheable(key = "'keywordSearch:' + #searchString")
    public TmdbNamedEntitySearchResultDto searchKeywords(String searchString) {
        return request(searchNamedEntityUri("/3/search/keyword", searchString),
                TmdbNamedEntitySearchResultDto.class);
    }

    @Cacheable(key = "'watchProviders:' + #mediaType + ':' + #watchRegion")
    public TmdbWatchProvidersDto getWatchProviders(
            AiMediaType mediaType,
            String watchRegion
    ) {
        return request(watchProvidersUri(mediaType, watchRegion), TmdbWatchProvidersDto.class);
    }

    @Cacheable(key = "'candidateFacts:' + #mediaType + ':' + #id + ':' + #facets")
    public TmdbCandidateFactsDto getCandidateFacts(
            AiMediaType mediaType,
            int id,
            Set<TmdbCandidateFacet> facets
    ) {
        return request(candidateFactsUri(mediaType, id, facets), TmdbCandidateFactsDto.class);
    }

    @Cacheable(key = "'combinedCredits:' + #personId")
    public TmdbCombinedCreditsDto getCombinedCredits(int personId) {
        return request(combinedCreditsUri(personId), TmdbCombinedCreditsDto.class);
    }

    @Cacheable(key = "'seriesDetail:' + #id")
    public TmdbSeriesDetailDto getSeries(int id) {
        return request(detailSeriesUri(id), TmdbSeriesDetailDto.class);
    }

    @Cacheable(key = "'movieDetail:' + #id")
    public TmdbMovieDetailDto getMovie(int id) {
        return request(detailMovieUri(id), TmdbMovieDetailDto.class);
    }

    @Cacheable(key = "'season:' + #seriesId + ':' + #season_number")
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

    private URI discoverMovieUri(TmdbDiscoverRequest discoverRequest) {
        UriComponentsBuilder builder = getDefaultComponentsBuilder()
                .path("/3/discover/movie");
        discoverRequest.applyTo(builder);
        return builder.build().encode().toUri();
    }

    private URI discoverSeriesUri(TmdbDiscoverRequest discoverRequest) {
        UriComponentsBuilder builder = getDefaultComponentsBuilder()
                .path("/3/discover/tv");
        discoverRequest.applyTo(builder);
        return builder.build().encode().toUri();
    }

    private URI similarMovieUri(int movieId) {
        return getDefaultComponentsBuilder()
                .path("/3/movie/" + movieId + "/similar")
                .queryParam("page", "1")
                .build()
                .encode()
                .toUri();
    }

    private URI similarSeriesUri(int seriesId) {
        return getDefaultComponentsBuilder()
                .path("/3/tv/" + seriesId + "/similar")
                .queryParam("page", "1")
                .build()
                .encode()
                .toUri();
    }

    private URI searchPeopleUri(String searchString) {
        return getDefaultComponentsBuilder()
                .path("/3/search/person")
                .queryParam("query", searchString)
                .queryParam("page", "1")
                .queryParam("include_adult", false)
                .build()
                .encode()
                .toUri();
    }

    private URI searchNamedEntityUri(String path, String searchString) {
        return getDefaultComponentsBuilder()
                .path(path)
                .queryParam("query", searchString)
                .queryParam("page", "1")
                .build()
                .encode()
                .toUri();
    }

    private URI watchProvidersUri(AiMediaType mediaType, String watchRegion) {
        String endpoint = mediaType == AiMediaType.MOVIE ? "movie" : "tv";
        return getDefaultComponentsBuilder()
                .path("/3/watch/providers/" + endpoint)
                .queryParam("watch_region", watchRegion)
                .build()
                .encode()
                .toUri();
    }

    private URI candidateFactsUri(
            AiMediaType mediaType,
            int id,
            Set<TmdbCandidateFacet> facets
    ) {
        String endpoint = mediaType == AiMediaType.MOVIE ? "movie" : "tv";
        UriComponentsBuilder builder = getDefaultComponentsBuilder()
                .path("/3/" + endpoint + "/" + id);
        String appended = facets.stream()
                .sorted()
                .map(TmdbCandidateFacet::parameterValue)
                .distinct()
                .collect(Collectors.joining(","));
        if (!appended.isEmpty()) {
            builder.queryParam("append_to_response", appended);
        }
        return builder.build().encode().toUri();
    }

    private URI combinedCreditsUri(int personId) {
        return getDefaultComponentsBuilder()
                .path("/3/person/" + personId + "/combined_credits")
                .build()
                .encode()
                .toUri();
    }

    private UriComponentsBuilder getDefaultComponentsBuilder() {
        String tmdbServiceKey = apiKeySupplier.get();

        if (tmdbServiceKey == null || tmdbServiceKey.isBlank()) {
            throw new MissingTmdbKeyException("TmdbKey is null");
        }

        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("api_key", tmdbServiceKey)
                .queryParam("language", "en-US");
    }

    private <T> T request(URI uri, Class<T> responseType) {
        try {
            if (interactionLoggingEnabled) {
                LOGGER.info("TMDB request\n{}", sanitizedUri(uri));
            }
            String response = httpTransport.get(uri);
            if (interactionLoggingEnabled) {
                LOGGER.info("TMDB response [{}]\n{}", uri.getPath(), response);
            }
            return objectMapper.readValue(response, responseType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("TMDB request was interrupted");
        } catch (Exception e) {
            LOGGER.warn("TMDB request or response processing failed");
        }
        return null;
    }

    static URI sanitizedUri(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam("api_key")
                .build(true)
                .toUri();
    }

    private URI seasonUri(int seriesId, int season_number) {
        return getDefaultComponentsBuilder()
                .path("/3/tv/" + seriesId + "/season/" + season_number)
                .build()
                .encode()
                .toUri();
    }
}
