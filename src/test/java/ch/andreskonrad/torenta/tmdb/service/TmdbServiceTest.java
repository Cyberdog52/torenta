package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.preference.dto.UserPreference;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMoviesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesSearchResultDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TmdbServiceTest {

    private static final String API_KEY = "TMDB_API_KEY_PLACEHOLDER";

    @Test
    void searchSeries_encodesQueryAndParsesResponse() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"results":[{"id":1402,"name":"The Walking Dead"}]}
                        """,
                requestedUri
        );

        TmdbSeriesSearchResultDto result = service.searchSeries("The 100% & MORE");

        assertNotNull(result);
        assertEquals(1402, result.getResults().getFirst().getId());
        assertEquals("The Walking Dead", result.getResults().getFirst().getName());
        assertEquals(
                "https://api.themoviedb.org/3/search/tv"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US"
                        + "&query=the%20100%25%20%26%20more&page=1",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void searchMovies_buildsUriAndParsesResponse() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"results":[{"id":550,"title":"Fight Club"}]}
                        """,
                requestedUri
        );

        TmdbMoviesSearchResultDto result = service.searchMovies("Fight Club");

        assertNotNull(result);
        assertEquals(550, result.getResults().getFirst().getId());
        assertEquals("Fight Club", result.getResults().getFirst().getTitle());
        assertEquals(
                "https://api.themoviedb.org/3/search/movie"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US"
                        + "&query=fight%20club&page=1&include_adult=false",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void getSeries_buildsUriAndParsesResponse() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"id":1402,"name":"The Walking Dead"}
                        """,
                requestedUri
        );

        TmdbSeriesDetailDto result = service.getSeries(1402);

        assertNotNull(result);
        assertEquals(1402, result.getId());
        assertEquals("The Walking Dead", result.getName());
        assertEquals(
                "https://api.themoviedb.org/3/tv/1402"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void getMovie_buildsUriAndParsesResponse() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"id":550,"title":"Fight Club"}
                        """,
                requestedUri
        );

        TmdbMovieDetailDto result = service.getMovie(550);

        assertNotNull(result);
        assertEquals(550, result.getId());
        assertEquals("Fight Club", result.getTitle());
        assertEquals(
                "https://api.themoviedb.org/3/movie/550"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void getEpisodes_buildsUriAndParsesResponse() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"episodes":[{"id":63056,"episode_number":1,"name":"Days Gone Bye"}]}
                        """,
                requestedUri
        );

        TmdbEpisodeDto[] result = service.getEpisodes(1402, 1);

        assertEquals(1, result.length);
        assertEquals(63056, result[0].getId());
        assertEquals("Days Gone Bye", result[0].getName());
        assertEquals(
                "https://api.themoviedb.org/3/tv/1402/season/1"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void malformedJson_returnsNull() {
        TmdbService service = serviceReturning("{not-json", new AtomicReference<>());

        assertNull(service.searchSeries("anything"));
    }

    @Test
    void transportFailure_returnsNull() {
        TmdbHttpTransport failingTransport = _ -> {
            throw new IOException("Offline transport failure");
        };
        TmdbService service = new TmdbService(preferenceService(), failingTransport, new ObjectMapper());

        assertNull(service.getMovie(550));
    }

    @Test
    void getEpisodes_withoutEpisodes_returnsEmptyFallback() {
        TmdbService service = serviceReturning("{}", new AtomicReference<>());

        assertEquals(0, service.getEpisodes(1402, 1).length);
    }

    @Test
    void getEpisodes_withMalformedJson_returnsEmptyFallback() {
        TmdbService service = serviceReturning("{not-json", new AtomicReference<>());

        assertEquals(0, service.getEpisodes(1402, 1).length);
    }

    private TmdbService serviceReturning(String response, AtomicReference<URI> requestedUri) {
        TmdbHttpTransport transport = uri -> {
            requestedUri.set(uri);
            return response;
        };
        return new TmdbService(preferenceService(), transport, new ObjectMapper());
    }

    private PreferenceService preferenceService() {
        PreferenceService preferenceService = mock(PreferenceService.class);
        when(preferenceService.loadPreferences()).thenReturn(new UserPreference(null, API_KEY));
        return preferenceService;
    }
}
