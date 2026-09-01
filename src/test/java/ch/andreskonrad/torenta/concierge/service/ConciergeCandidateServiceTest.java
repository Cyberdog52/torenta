package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.tmdb.dto.*;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterRegistry;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequest;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequestFactory;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbNameResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConciergeCandidateServiceTest {

    @Mock
    private TmdbService tmdbService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ConciergeCandidateService service;

    @BeforeEach
    void setUp() {
        GenreIdMapper genres = new GenreIdMapper();
        TmdbDiscoverFilterRegistry registry = new TmdbDiscoverFilterRegistry();
        TmdbNameResolver resolver = new TmdbNameResolver(tmdbService, genres);
        service = new ConciergeCandidateService(
                tmdbService,
                genres,
                new TmdbDiscoverRequestFactory(registry, resolver)
        );
    }

    @Test
    void findCandidates_discoversBothMediaWithTypedFilters() {
        when(tmdbService.discoverMovies(any())).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":1,"title":"Movie","genre_ids":[878],"popularity":10}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.discoverSeries(any())).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":2,"name":"Series","genre_ids":[10765],"popularity":20}]}
                        """,
                TmdbSeriesSearchResultDto.class
        ));
        SearchIntent intent = intent(
                AiMediaType.ANY,
                List.of(
                        new NumericFilterCriterion(
                                NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 7.0, "rated 7"
                        ),
                        new NumericFilterCriterion(
                                NumericFilterKey.RUNTIME, FilterOperator.LTE, 120.0, "under 120 minutes"
                        )
                ),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.GENRE, List.of("Science Fiction"),
                        FilterPolarity.INCLUDE, FilterMatch.ALL, "science fiction"
                ))
        );

        List<MediaCandidate> candidates = service.findCandidates(intent);

        assertEquals(List.of("SERIES:2", "MOVIE:1"), candidates.stream()
                .map(MediaCandidate::candidateKey).toList());
        verify(tmdbService).discoverMovies(any(TmdbDiscoverRequest.class));
        verify(tmdbService).discoverSeries(any(TmdbDiscoverRequest.class));
        verify(tmdbService, never()).getCandidateFacts(any(), anyInt(), anySet());
    }

    @Test
    void findCandidates_resolvesMovieCastThroughTmdb() {
        when(tmdbService.searchPeople("Jack Nicholson")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":514,"name":"Jack Nicholson"}]}
                        """,
                TmdbPersonSearchResultDto.class
        ));
        when(tmdbService.discoverMovies(any())).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":75,"title":"Mars Attacks!","genre_ids":[35]}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        SearchIntent intent = intent(
                AiMediaType.MOVIE,
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.CAST, List.of("Jack Nicholson"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "jack nicholson"
                ))
        );

        List<MediaCandidate> candidates = service.findCandidates(intent);

        assertEquals(List.of("Mars Attacks!"), candidates.stream().map(MediaCandidate::title).toList());
        verify(tmdbService).searchPeople("Jack Nicholson");
        verify(tmdbService, never()).getCandidateFacts(any(), anyInt(), anySet());
    }

    @Test
    void findCandidates_keepsUnresolvedNamedFilterForFactualRanking() {
        when(tmdbService.searchCompanies("Unknown Studio")).thenReturn(objectMapper.readValue(
                """
                        {"results":[]}
                        """,
                TmdbNamedEntitySearchResultDto.class
        ));
        when(tmdbService.discoverMovies(any())).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":1,"title":"Candidate"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.getCandidateFacts(eq(AiMediaType.MOVIE), eq(1), anySet()))
                .thenReturn(objectMapper.readValue(
                        """
                                {"production_companies":[{"id":2,"name":"Known Studio"}]}
                                """,
                        TmdbCandidateFactsDto.class
                ));
        SearchIntent intent = intent(
                AiMediaType.MOVIE,
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.COMPANY, List.of("Unknown Studio"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "Unknown Studio"
                ))
        );

        List<MediaCandidate> candidates = service.findCandidates(intent);

        assertEquals(List.of("Known Studio"), candidates.getFirst().facts().companies());
        assertEquals(
                java.util.Set.of(NamedFilterKey.COMPANY),
                candidates.getFirst().facts().knownNamedFacts()
        );
        verify(tmdbService).discoverMovies(any());
    }

    @Test
    void findCandidates_similarPathAppliesKnownObjectiveFacts() {
        when(tmdbService.searchMovies("Andor")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":10,"title":"Andor"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.similarMovies(10)).thenReturn(objectMapper.readValue(
                """
                        {"results":[
                          {"id":11,"title":"Match","vote_average":8},
                          {"id":12,"title":"Too low","vote_average":6}
                        ]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), "Andor",
                List.of(new NumericFilterCriterion(
                        NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 7.0, "rating above 7"
                )),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );

        List<MediaCandidate> candidates = service.findCandidates(intent);

        assertEquals(List.of("MOVIE:11"), candidates.stream()
                .map(MediaCandidate::candidateKey).toList());
        verify(tmdbService, never()).discoverMovies(any());
    }

    @Test
    void findCandidates_similarPathWithCastEnrichesWithoutPeopleLookup() {
        when(tmdbService.searchMovies("Andor")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":10,"title":"Andor"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.similarMovies(10)).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":11,"title":"Candidate","popularity":8}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.getCandidateFacts(eq(AiMediaType.MOVIE), eq(11), anySet()))
                .thenReturn(objectMapper.readValue(
                        """
                                {"credits":{"cast":[{"id":514,"name":"Jack Nicholson"}]}}
                                """,
                        TmdbCandidateFactsDto.class
                ));
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE,
                List.of(),
                "Andor",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.CAST, List.of("Jack Nicholson"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "jack nicholson"
                )),
                List.of()
        );

        List<MediaCandidate> candidates = service.findCandidates(intent);

        assertEquals(List.of("Jack Nicholson"), candidates.getFirst().facts().cast());
        verify(tmdbService, never()).searchPeople(anyString());
        verify(tmdbService).getCandidateFacts(eq(AiMediaType.MOVIE), eq(11), anySet());
    }

    @Test
    void findCandidates_similarAnyHandlesEmptySearchResults() {
        when(tmdbService.searchMovies("Unknown")).thenReturn(new TmdbMoviesSearchResultDto());
        when(tmdbService.searchSeries("Unknown")).thenReturn(new TmdbSeriesSearchResultDto());
        SearchIntent intent = new SearchIntent(
                AiMediaType.ANY, List.of(), "Unknown", List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );

        assertEquals(List.of(), service.findCandidates(intent));

        verify(tmdbService, never()).similarMovies(anyInt());
        verify(tmdbService, never()).similarSeries(anyInt());
    }

    @Test
    void findCandidates_similarPathFiltersObjectiveFactsAndGenrePolarity() {
        when(tmdbService.searchMovies("Seed")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":10,"title":"Seed"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.similarMovies(10)).thenReturn(objectMapper.readValue(
                """
                        {"results":[
                          {"id":11,"title":"Match","release_date":"2020-01-01",
                           "original_language":"en","genre_ids":[878,53],
                           "vote_average":8,"vote_count":100},
                          {"id":12,"title":"Wrong language","release_date":"2020-01-01",
                           "original_language":"de","genre_ids":[878,53],
                           "vote_average":8,"vote_count":100},
                          {"id":13,"title":"Excluded genre","release_date":"2020-01-01",
                           "original_language":"en","genre_ids":[35],
                           "vote_average":8,"vote_count":100},
                          {"id":14,"title":"Unknown year","release_date":"invalid",
                           "original_language":"en","genre_ids":[878,53],
                           "vote_average":8,"vote_count":100}
                        ]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), "Seed",
                List.of(
                        new NumericFilterCriterion(
                                NumericFilterKey.YEAR, FilterOperator.EQ, 2020.0, "2020"
                        ),
                        new NumericFilterCriterion(
                                NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 7.0, "rated"
                        ),
                        new NumericFilterCriterion(
                                NumericFilterKey.VOTE_COUNT, FilterOperator.LTE, 200.0, "votes"
                        ),
                        new NumericFilterCriterion(
                                NumericFilterKey.PAGE, FilterOperator.EQ, 1.0, "page"
                        )
                ),
                List.of(),
                List.of(new TextFilterCriterion(
                        TextFilterKey.ORIGINAL_LANGUAGE, "en", "English"
                )),
                List.of(),
                List.of(
                        new NamedFilterCriterion(
                                NamedFilterKey.GENRE, List.of("Science Fiction", "Thriller"),
                                FilterPolarity.INCLUDE, FilterMatch.ALL, "genres"
                        ),
                        new NamedFilterCriterion(
                                NamedFilterKey.GENRE, List.of("Comedy"),
                                FilterPolarity.EXCLUDE, FilterMatch.ANY, "not comedy"
                        )
                ),
                List.of()
        );

        assertEquals(List.of("MOVIE:11", "MOVIE:14"), service.findCandidates(intent).stream()
                .map(MediaCandidate::candidateKey).toList());
    }

    @Test
    void findCandidates_enrichesSeriesRuntimeAndAllFactualFacets() {
        when(tmdbService.searchSeries("Seed")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":10,"name":"Seed"}]}
                        """,
                TmdbSeriesSearchResultDto.class
        ));
        when(tmdbService.similarSeries(10)).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":11,"name":"Candidate","popularity":8}]}
                        """,
                TmdbSeriesSearchResultDto.class
        ));
        when(tmdbService.getCandidateFacts(eq(AiMediaType.SERIES), eq(11), anySet()))
                .thenReturn(objectMapper.readValue(
                        """
                                {
                                  "episode_run_time":[45],
                                  "genres":[{"id":18,"name":"Drama"}],
                                  "credits":{
                                    "cast":[{"id":1,"name":"Actor"},{"id":2,"name":null}],
                                    "crew":[{"id":3,"name":"Creator"}]
                                  },
                                  "production_companies":[{"id":4,"name":"Studio"}],
                                  "keywords":{"results":[{"id":5,"name":"space"}]},
                                  "networks":[{"id":6,"name":"Network"}],
                                  "watch/providers":{"results":{
                                    "US":{
                                      "flatrate":[{"provider_id":8,"provider_name":"Netflix"}],
                                      "rent":[{"provider_id":9,"provider_name":"Store"}]
                                    }
                                  }},
                                  "content_ratings":{"results":[
                                    {"iso_3166_1":"US","rating":"TV-14"},
                                    {"iso_3166_1":"CH","rating":""}
                                  ]}
                                }
                                """,
                        TmdbCandidateFactsDto.class
                ));
        SearchIntent intent = new SearchIntent(
                AiMediaType.SERIES, List.of(), "Seed",
                List.of(new NumericFilterCriterion(
                        NumericFilterKey.RUNTIME, FilterOperator.LTE, 60.0, "short"
                )),
                List.of(),
                List.of(new TextFilterCriterion(
                        TextFilterKey.CERTIFICATION, "TV-14", "rating"
                )),
                List.of(),
                List.of(
                        named(NamedFilterKey.CAST, "Actor"),
                        named(NamedFilterKey.CREW, "Creator"),
                        named(NamedFilterKey.PEOPLE, "Actor"),
                        named(NamedFilterKey.COMPANY, "Studio"),
                        named(NamedFilterKey.KEYWORD, "space"),
                        named(NamedFilterKey.NETWORK, "Network"),
                        named(NamedFilterKey.WATCH_PROVIDER, "Netflix")
                ),
                List.of()
        );

        MediaCandidate candidate = service.findCandidates(intent).getFirst();

        assertEquals(45, candidate.runtime());
        assertEquals(List.of("Actor"), candidate.facts().cast());
        assertEquals(List.of("Creator"), candidate.facts().crew());
        assertEquals(List.of("space"), candidate.facts().keywords());
        assertEquals(List.of("Netflix", "Store"), candidate.facts().watchProviders());
        assertEquals(List.of("TV-14"), candidate.facts().certifications());
        assertEquals(Set.of(
                NamedFilterKey.CAST, NamedFilterKey.CREW, NamedFilterKey.PEOPLE,
                NamedFilterKey.COMPANY, NamedFilterKey.KEYWORD, NamedFilterKey.NETWORK,
                NamedFilterKey.WATCH_PROVIDER
        ), candidate.facts().knownNamedFacts());
    }

    @Test
    void findCandidates_rejectsNullTmdbResponsesAndKeepsCandidateForNullDetails() {
        when(tmdbService.discoverMovies(any())).thenReturn(null);
        SearchIntent discoverIntent = intent(AiMediaType.MOVIE, List.of(), List.of());
        assertThrows(IllegalStateException.class, () -> service.findCandidates(discoverIntent));

        reset(tmdbService);
        when(tmdbService.searchMovies("Seed")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":10,"title":"Seed"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.similarMovies(10)).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":11,"title":"Candidate"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.getCandidateFacts(any(), anyInt(), anySet())).thenReturn(null);
        SearchIntent similarIntent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), "Seed", List.of(), List.of(), List.of(),
                List.of(), List.of(named(NamedFilterKey.CAST, "Actor")), List.of()
        );

        assertEquals("Candidate", service.findCandidates(similarIntent).getFirst().title());
    }

    @Test
    void findCandidates_deduplicatesSkipsNullsUsesFallbackTitlesAndCapsShortlist() {
        String results = IntStream.rangeClosed(1, 45)
                .mapToObj(index -> index == 1
                        ? """
                        {"id":1,"title":" ","original_title":"Fallback title","popularity":100}
                        """.trim()
                        : "{\"id\":" + index + ",\"title\":\"Movie " + index
                        + "\",\"popularity\":" + (100 - index) + "}")
                .collect(java.util.stream.Collectors.joining(","));
        when(tmdbService.discoverMovies(any())).thenReturn(objectMapper.readValue(
                "{\"results\":[null," + results
                        + ",{\"id\":1,\"title\":\"Duplicate\",\"popularity\":0}]}",
                TmdbMoviesSearchResultDto.class
        ));

        List<MediaCandidate> candidates = service.findCandidates(
                intent(AiMediaType.MOVIE, List.of(), List.of())
        );

        assertEquals(40, candidates.size());
        assertEquals(40, candidates.stream().map(MediaCandidate::candidateKey).distinct().count());
        assertEquals("Fallback title", candidates.getFirst().title());
    }

    @Test
    void findCandidates_enrichesMovieRuntimeReleaseCertificationsAndKeywordVariant() {
        when(tmdbService.searchMovies("Seed")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":10,"title":"Seed"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.similarMovies(10)).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":11,"title":"Candidate"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.getCandidateFacts(eq(AiMediaType.MOVIE), eq(11), anySet()))
                .thenReturn(objectMapper.readValue(
                        """
                                {
                                  "runtime":110,
                                  "keywords":{"keywords":[{"id":1,"name":"heist"}]},
                                  "release_dates":{"results":[
                                    {"release_dates":[
                                      {"certification":"PG-13"},
                                      {"certification":""}
                                    ]}
                                  ]},
                                  "watch/providers":{"results":{}}
                                }
                                """,
                        TmdbCandidateFactsDto.class
                ));
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), "Seed",
                List.of(new NumericFilterCriterion(
                        NumericFilterKey.RUNTIME, FilterOperator.LTE, 120.0, "runtime"
                )),
                List.of(),
                List.of(new TextFilterCriterion(
                        TextFilterKey.CERTIFICATION, "PG-13", "rating"
                )),
                List.of(),
                List.of(
                        named(NamedFilterKey.KEYWORD, "heist"),
                        named(NamedFilterKey.WATCH_PROVIDER, "provider")
                ),
                List.of()
        );

        MediaCandidate candidate = service.findCandidates(intent).getFirst();

        assertEquals(110, candidate.runtime());
        assertEquals(List.of("heist"), candidate.facts().keywords());
        assertEquals(List.of("PG-13"), candidate.facts().certifications());
        assertEquals(List.of(), candidate.facts().watchProviders());
    }

    @Test
    void findCandidates_enrichmentIsCappedEvenWhenMoreCandidatesNeedFacts() {
        String results = IntStream.rangeClosed(1, 20)
                .mapToObj(index -> "{\"id\":" + index + ",\"title\":\"Movie " + index
                        + "\",\"popularity\":" + (100 - index) + "}")
                .collect(java.util.stream.Collectors.joining(","));
        when(tmdbService.discoverMovies(any())).thenReturn(objectMapper.readValue(
                "{\"results\":[" + results + "]}",
                TmdbMoviesSearchResultDto.class
        ));
        when(tmdbService.getCandidateFacts(any(), anyInt(), anySet()))
                .thenReturn(new TmdbCandidateFactsDto());
        SearchIntent intent = intent(
                AiMediaType.MOVIE,
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.COMPANY, List.of("Unknown Studio"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "unknown studio"
                ))
        );

        service.findCandidates(intent);

        verify(tmdbService, atMost(12)).getCandidateFacts(eq(AiMediaType.MOVIE), anyInt(), anySet());
    }

    private SearchIntent intent(
            AiMediaType mediaType,
            List<NumericFilterCriterion> numeric,
            List<NamedFilterCriterion> named
    ) {
        return new SearchIntent(
                mediaType, List.of(), null, numeric, List.of(), List.of(),
                List.of(), named, List.of()
        );
    }

    private NamedFilterCriterion named(NamedFilterKey key, String value) {
        return new NamedFilterCriterion(
                key, List.of(value), FilterPolarity.INCLUDE, FilterMatch.ANY, value
        );
    }
}
