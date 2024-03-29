package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.tmdb.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@CacheConfig(cacheNames={CustomCacheConfig.TMDB_CACHE_NAME})
public class TmdbService {

    @Value("${ch.andreskonrad.torenta.tmdb.service.key}")
    private String apiKey;
    private final String host = "api.themoviedb.org";
    private final String scheme = "https";

    private static final RequestThrottler requestThrottler = new RequestThrottler(9, 1000);


    @Cacheable
    public TmdbSeriesSearchResultDto searchSeries(String searchString) {
        String jsonStringResponse = searchSeriesRequest(searchString);
        try {
            return new ObjectMapper().readValue(jsonStringResponse, TmdbSeriesSearchResultDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Cacheable
    public TmdbMoviesSearchResultDto searchMovies(String searchString) {
        String jsonStringResponse = searchMovieRequest(searchString);
        try {
            return new ObjectMapper().readValue(jsonStringResponse, TmdbMoviesSearchResultDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Cacheable
    public TmdbSeriesDetailDto getSeries(int id) {
        String jsonStringResponse = detailSeriesRequest(id);
        try {
            return new ObjectMapper().readValue(jsonStringResponse, TmdbSeriesDetailDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Cacheable
    public TmdbMovieDetailDto getMovie(int id) {
        String jsonStringResponse = detailMovieRequest(id);
        try {
            return new ObjectMapper().readValue(jsonStringResponse, TmdbMovieDetailDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Cacheable
    public TmdbEpisodeDto[] getEpisodes(int seriesId, int season_number) {
        String jsonStringResponse = seasonRequest(seriesId, season_number);
        try {
            return new ObjectMapper()
                    .readValue(jsonStringResponse, TmdbSeasonDto.class)
                    .getEpisodes();
        } catch (Throwable t) {
            t.printStackTrace();
            return new TmdbEpisodeDto[0];
        }
    }

    private String detailSeriesRequest(int id) {
        return httpGet(getDefaultComponentsBuilder()
                .path("/3/tv/" + String.valueOf(id))
                .build()
                .encode()
                .toUri());
    }

    private String detailMovieRequest(int id) {
        return httpGet(getDefaultComponentsBuilder()
                .path("/3/movie/" + String.valueOf(id))
                .build()
                .encode()
                .toUri());
    }

    private String searchSeriesRequest(String searchString) {
        return httpGet(getDefaultComponentsBuilder()
                .path("/3/search/tv")
                .queryParam("query", searchString.toLowerCase())
                .queryParam("page", "1")
                .build()
                .encode()
                .toUri());
    }

    private String searchMovieRequest(String searchString) {
        return httpGet(getDefaultComponentsBuilder()
                .path("/3/search/movie")
                .queryParam("query", searchString.toLowerCase())
                .queryParam("page", "1")
                .queryParam("include_adult", false)
                .build()
                .encode()
                .toUri());
    }

    private UriComponentsBuilder getDefaultComponentsBuilder() {
        return UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(host)
                .queryParam("api_key", apiKey)
                .queryParam("language", "en-US");
    }

    private String httpGet(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        try {
            requestThrottler.throttle();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String seasonRequest(int seriesId, int season_number) {
        return httpGet(getDefaultComponentsBuilder()
                .path("/3/tv/" + String.valueOf(seriesId) + "/season/" + String.valueOf(season_number))
                .build()
                .encode()
                .toUri());
    }
}
