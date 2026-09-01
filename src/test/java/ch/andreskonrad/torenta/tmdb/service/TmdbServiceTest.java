package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.preference.dto.UserPreference;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import ch.andreskonrad.torenta.tmdb.dto.*;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterRegistry;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequest;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequestFactory;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbNameResolver;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void discoverMovies_addsObjectiveFilters() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"results":[]}
                        """,
                requestedUri
        );

        TmdbNameResolver resolver = mock(TmdbNameResolver.class);
        when(resolver.resolve(any(), anyString(), anyString(), any(), any()))
                .thenReturn(OptionalInt.of(878), OptionalInt.of(53), OptionalInt.of(514));
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), null,
                List.of(
                        new NumericFilterCriterion(
                                NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 7.0, "rated 7"
                        ),
                        new NumericFilterCriterion(
                                NumericFilterKey.RUNTIME, FilterOperator.LTE, 120.0, "under 120"
                        )
                ),
                List.of(
                        new DateFilterCriterion(
                                DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.GTE,
                                "2016-01-01", "after 2015"
                        ),
                        new DateFilterCriterion(
                                DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.LTE,
                                "2025-12-31", "before 2026"
                        )
                ),
                List.of(new TextFilterCriterion(
                        TextFilterKey.ORIGINAL_LANGUAGE, "en", "English"
                )),
                List.of(),
                List.of(
                        new NamedFilterCriterion(
                                NamedFilterKey.GENRE, List.of("Science Fiction", "Thriller"),
                                FilterPolarity.INCLUDE, FilterMatch.ALL, "sci-fi thriller"
                        ),
                        new NamedFilterCriterion(
                                NamedFilterKey.CAST, List.of("Jack Nicholson"),
                                FilterPolarity.INCLUDE, FilterMatch.ANY, "Jack Nicholson"
                        )
                ),
                List.of()
        );
        service.discoverMovies(request(intent, AiMediaType.MOVIE, resolver));

        assertEquals(
                "https://api.themoviedb.org/3/discover/movie"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US&page=1"
                        + "&sort_by=popularity.desc&include_adult=false&include_video=false"
                        + "&vote_average.gte=7&with_runtime.lte=120"
                        + "&primary_release_date.gte=2016-01-01"
                        + "&primary_release_date.lte=2025-12-31&with_original_language=en"
                        + "&with_genres=878,53&with_cast=514",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void discoverSeries_usesConfigurableBaseUrlAndTvDateFilters() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbHttpTransport transport = uri -> {
            requestedUri.set(uri);
            return """
                    {"results":[]}
                    """;
        };
        TmdbService service = new TmdbService(
                API_KEY, "http://localhost:19090", transport, new ObjectMapper()
        );

        TmdbNameResolver resolver = mock(TmdbNameResolver.class);
        when(resolver.resolve(any(), anyString(), anyString(), any(), any()))
                .thenReturn(OptionalInt.of(10765));
        SearchIntent intent = new SearchIntent(
                AiMediaType.SERIES, List.of(), null, List.of(),
                List.of(
                        new DateFilterCriterion(
                                DateFilterKey.FIRST_AIR_DATE, FilterOperator.GTE,
                                "1990-01-01", "the nineties"
                        ),
                        new DateFilterCriterion(
                                DateFilterKey.FIRST_AIR_DATE, FilterOperator.LTE,
                                "1999-12-31", "the nineties"
                        )
                ),
                List.of(), List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.GENRE, List.of("Science Fiction"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "science fiction"
                )),
                List.of()
        );
        service.discoverSeries(request(intent, AiMediaType.SERIES, resolver));

        assertEquals(
                "http://localhost:19090/3/discover/tv"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US&page=1"
                        + "&sort_by=popularity.desc&include_adult=false"
                        + "&include_null_first_air_dates=false"
                        + "&first_air_date.gte=1990-01-01&first_air_date.lte=1999-12-31"
                        + "&with_genres=10765",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void similarAndPeopleEndpoints_buildUrisAndParseResponses() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"results":[{"id":1}]}
                        """,
                requestedUri
        );

        assertNotNull(service.similarMovies(550));
        assertEquals(
                "https://api.themoviedb.org/3/movie/550/similar"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US&page=1",
                requestedUri.get().toASCIIString()
        );
        assertNotNull(service.similarSeries(2316));
        assertEquals(
                "https://api.themoviedb.org/3/tv/2316/similar"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US&page=1",
                requestedUri.get().toASCIIString()
        );
        TmdbPersonSearchResultDto people = service.searchPeople("Diego Luna");
        assertEquals(1, people.getResults().getFirst().getId());
        assertEquals(
                "https://api.themoviedb.org/3/search/person"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US"
                        + "&query=Diego%20Luna&page=1&include_adult=false",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void combinedCredits_buildsUriAndParsesCast() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning(
                """
                        {"cast":[{"id":1,"media_type":"movie","title":"Movie"}]}
                        """,
                requestedUri
        );

        TmdbCombinedCreditsDto credits = service.getCombinedCredits(100);

        assertEquals("Movie", credits.getCast().getFirst().getTitle());
        assertEquals(
                "https://api.themoviedb.org/3/person/100/combined_credits"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void sanitizedUri_removesApiKeyButKeepsSearchParameters() {
        URI sanitized = TmdbService.sanitizedUri(URI.create(
                "https://api.themoviedb.org/3/discover/movie"
                        + "?api_key=secret&language=en-US&with_genres=878"
        ));

        assertEquals(
                "https://api.themoviedb.org/3/discover/movie?language=en-US&with_genres=878",
                sanitized.toASCIIString()
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
    void watchProviderAndCandidateFactsUrisUseMediaSpecificPathsAndFacets() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        TmdbService service = serviceReturning("{}", requestedUri);

        assertNotNull(service.getWatchProviders(AiMediaType.MOVIE, "CH"));
        assertEquals(
                "https://api.themoviedb.org/3/watch/providers/movie"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US&watch_region=CH",
                requestedUri.get().toASCIIString()
        );
        assertNotNull(service.getWatchProviders(AiMediaType.SERIES, "US"));
        assertEquals(
                "https://api.themoviedb.org/3/watch/providers/tv"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US&watch_region=US",
                requestedUri.get().toASCIIString()
        );

        assertNotNull(service.getCandidateFacts(AiMediaType.MOVIE, 1, Set.of()));
        assertEquals(
                "https://api.themoviedb.org/3/movie/1"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US",
                requestedUri.get().toASCIIString()
        );
        assertNotNull(service.getCandidateFacts(
                AiMediaType.SERIES,
                2,
                Set.of(
                        TmdbCandidateFacet.WATCH_PROVIDERS,
                        TmdbCandidateFacet.CREDITS,
                        TmdbCandidateFacet.KEYWORDS
                )
        ));
        assertEquals(
                "https://api.themoviedb.org/3/tv/2"
                        + "?api_key=TMDB_API_KEY_PLACEHOLDER&language=en-US"
                        + "&append_to_response=credits,keywords,watch/providers",
                requestedUri.get().toASCIIString()
        );
    }

    @Test
    void interruptedTransportRestoresInterruptFlagAndReturnsNull() {
        TmdbHttpTransport interruptedTransport = _ -> {
            throw new InterruptedException("interrupted");
        };
        TmdbService service = new TmdbService(
                preferenceService(), interruptedTransport, new ObjectMapper()
        );

        try {
            assertNull(service.getMovie(550));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            assertTrue(Thread.interrupted());
        }
    }

    @Test
    void missingTmdbKey_throwsException() {
        PreferenceService missingKeyService = mock(PreferenceService.class);
        when(missingKeyService.loadPreferences()).thenReturn(new UserPreference(null, null));
        TmdbService service = new TmdbService(missingKeyService, _ -> "{}", new ObjectMapper());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.searchSeries("anything"));
        assertEquals("TmdbKey is null", exception.getMessage());
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

    private TmdbDiscoverRequest request(
            SearchIntent intent,
            AiMediaType mediaType,
            TmdbNameResolver resolver
    ) {
        return new TmdbDiscoverRequestFactory(
                new TmdbDiscoverFilterRegistry(),
                resolver
        ).create(intent, mediaType);
    }

    private PreferenceService preferenceService() {
        PreferenceService preferenceService = mock(PreferenceService.class);
        when(preferenceService.loadPreferences()).thenReturn(new UserPreference(null, API_KEY));
        return preferenceService;
    }
}
