package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.concierge.service.provider.AiProvider;
import ch.andreskonrad.torenta.concierge.service.provider.AiProviderFactory;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConciergeServiceTest {

    @Mock
    private AiProviderFactory providerFactory;

    @Mock
    private AiProvider provider;

    @Mock
    private ConciergeCandidateService candidateService;

    private AiConciergeService service;

    @BeforeEach
    void setUp() {
        service = new AiConciergeService(
                providerFactory,
                candidateService,
                new SearchIntentNormalizer(new TmdbDiscoverFilterRegistry())
        );
    }

    @Test
    void search_normalizesIntentAndReturnsOnlyValidAiRankedTmdbCandidates() {
        SearchIntent extracted = new SearchIntent(
                null,
                List.of(" dark "),
                null,
                List.of(),
                List.of(),
                List.of(new TextFilterCriterion(
                        TextFilterKey.ORIGINAL_LANGUAGE, "EN", "English"
                )),
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.GENRE, List.of("science fiction", "science-fiction"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "sci-fi"
                )),
                List.of()
        );
        MediaCandidate movie = candidate("MOVIE:1", AiMediaType.MOVIE, 1, "Movie");
        MediaCandidate series = candidate("SERIES:2", AiMediaType.SERIES, 2, "Series");
        MediaCandidate excluded = candidate("MOVIE:3", AiMediaType.MOVIE, 3, "Excluded");
        when(providerFactory.selectedProvider()).thenReturn(provider);
        when(provider.extractIntent("dark sci-fi")).thenReturn(extracted);
        when(candidateService.findCandidates(any())).thenReturn(List.of(movie, series, excluded));
        when(provider.rank(anyString(), any(), anyList())).thenReturn(List.of(
                new CandidateRanking("SERIES:2", 95, "Best subjective match"),
                new CandidateRanking("NOT_FROM_TMDB:9", 100, "Invented"),
                new CandidateRanking("MOVIE:1", 80, "Also suitable"),
                new CandidateRanking("MOVIE:3", 0, "Does not match"),
                new CandidateRanking("MOVIE:1", 70, "Duplicate")
        ));

        AiConciergeResponse response = service.search(" dark sci-fi ");

        assertEquals(AiMediaType.ANY, response.intent().mediaType());
        assertEquals(List.of("DARK"), response.intent().moods());
        assertTrue(response.intent().textFilters().isEmpty());
        assertEquals(List.of("science fiction", "science-fiction"),
                response.intent().namedFilters().getFirst().names());
        assertEquals(2, response.results().size());
        assertEquals("Series", response.results().getFirst().title());
        assertEquals(1, response.results().getFirst().rank());
        assertEquals("Movie", response.results().get(1).title());
        assertFalse(response.results().stream().anyMatch(result -> result.title().equals("Excluded")));
        verify(provider).extractIntent("dark sci-fi");
    }

    @Test
    void search_capsResultsAtTwenty() {
        SearchIntent intent = intent(AiMediaType.MOVIE);
        List<MediaCandidate> candidates = new ArrayList<>();
        List<CandidateRanking> rankings = new ArrayList<>();
        for (int index = 1; index <= 25; index++) {
            candidates.add(candidate("MOVIE:" + index, AiMediaType.MOVIE, index, "Movie " + index));
            rankings.add(new CandidateRanking("MOVIE:" + index, 100 - index, "Reason " + index));
        }
        when(providerFactory.selectedProvider()).thenReturn(provider);
        when(provider.extractIntent("movies")).thenReturn(intent);
        when(candidateService.findCandidates(any())).thenReturn(candidates);
        when(provider.rank(anyString(), any(), anyList())).thenReturn(rankings);

        AiConciergeResponse response = service.search("movies");

        assertEquals(20, response.results().size());
        assertEquals(20, response.results().getLast().rank());
    }

    @Test
    void search_returnsEmptyResponseWithoutRankingWhenTmdbHasNoCandidates() {
        SearchIntent intent = intent(AiMediaType.SERIES);
        when(providerFactory.selectedProvider()).thenReturn(provider);
        when(provider.extractIntent("series")).thenReturn(intent);
        when(candidateService.findCandidates(any())).thenReturn(List.of());

        AiConciergeResponse response = service.search("series");

        assertTrue(response.results().isEmpty());
        verify(provider, never()).rank(anyString(), any(), anyList());
    }

    @Test
    void search_omitsUnevidencedZeroYearsAndKeepsCorrectedActor() {
        SearchIntent hallucinated = new SearchIntent(
                AiMediaType.MOVIE,
                List.of("HUMOROUS"),
                null,
                List.of(
                        new NumericFilterCriterion(NumericFilterKey.YEAR, FilterOperator.GTE, 0.0, ""),
                        new NumericFilterCriterion(NumericFilterKey.YEAR, FilterOperator.LTE, 0.0, "")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.CAST, List.of("Jack Nicholson"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "jack nicholson"
                )),
                List.of()
        );
        when(providerFactory.selectedProvider()).thenReturn(provider);
        when(provider.extractIntent("a funny movie with jack nicholson"))
                .thenReturn(hallucinated);
        when(candidateService.findCandidates(any())).thenReturn(List.of());

        AiConciergeResponse response =
                service.search("a funny movie with jack nicholson");

        assertTrue(response.intent().numericFilters().isEmpty());
        assertEquals("Jack Nicholson",
                response.intent().namedFilters().getFirst().names().getFirst());
    }

    @Test
    void search_rejectsBlankAndOversizedPromptsBeforeCallingProvider() {
        assertThrows(IllegalArgumentException.class, () -> service.search(null));
        assertThrows(IllegalArgumentException.class, () -> service.search(" "));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.search("x".repeat(AiConciergeService.MAX_PROMPT_LENGTH + 1))
        );
        verifyNoInteractions(providerFactory, candidateService);
    }

    @Test
    void search_rejectsMissingExtractedIntentAndNullRankingPayload() {
        when(providerFactory.selectedProvider()).thenReturn(provider);
        when(provider.extractIntent("missing intent")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.search("missing intent"));

        SearchIntent intent = intent(AiMediaType.MOVIE);
        when(provider.extractIntent("null ranking payload")).thenReturn(intent);
        when(candidateService.findCandidates(intent)).thenReturn(List.of(
                candidate("MOVIE:1", AiMediaType.MOVIE, 1, "Movie")
        ));
        when(provider.rank(anyString(), any(), anyList())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.search("null ranking payload"));
    }

    @Test
    void search_returnsEmptyResultsWhenAllRankingsAreFilteredOut() {
        SearchIntent intent = intent(AiMediaType.MOVIE);
        when(providerFactory.selectedProvider()).thenReturn(provider);
        when(provider.extractIntent("no good matches")).thenReturn(intent);
        when(candidateService.findCandidates(any())).thenReturn(List.of(
                candidate("MOVIE:1", AiMediaType.MOVIE, 1, "Movie 1"),
                candidate("MOVIE:2", AiMediaType.MOVIE, 2, "Movie 2")
        ));
        when(provider.rank(anyString(), any(), anyList())).thenReturn(List.of(
                new CandidateRanking("MOVIE:1", 0, "No match"),
                new CandidateRanking("MOVIE:404", 88, "Unknown candidate")
        ));

        AiConciergeResponse response = service.search("no good matches");

        assertTrue(response.results().isEmpty());
    }

    @Test
    void search_ignoresNullMalformedAndDuplicateRankings() {
        SearchIntent intent = intent(AiMediaType.MOVIE);
        when(providerFactory.selectedProvider()).thenReturn(provider);
        when(provider.extractIntent("rank safely")).thenReturn(intent);
        when(candidateService.findCandidates(any())).thenReturn(List.of(
                candidate("MOVIE:1", AiMediaType.MOVIE, 1, "Movie 1"),
                candidate("MOVIE:2", AiMediaType.MOVIE, 2, "Movie 2")
        ));
        when(provider.rank(anyString(), any(), anyList())).thenReturn(Arrays.asList(
                null,
                new CandidateRanking("MOVIE:1", 101, "Too high"),
                new CandidateRanking("MOVIE:1", 80, null),
                new CandidateRanking("MOVIE:1", 80, " "),
                new CandidateRanking("MOVIE:2", 70, " Valid reason "),
                new CandidateRanking("MOVIE:2", 60, "Duplicate")
        ));

        AiConciergeResponse response = service.search("rank safely");

        assertEquals(1, response.results().size());
        assertEquals("Movie 2", response.results().getFirst().title());
        assertEquals("Valid reason", response.results().getFirst().explanation());
    }

    private SearchIntent intent(AiMediaType mediaType) {
        return new SearchIntent(
                mediaType, List.of(), null, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }

    private MediaCandidate candidate(String key, AiMediaType type, int id, String title) {
        return new MediaCandidate(
                key, type, id, title, "Overview", List.of(), "2020-01-01",
                "en", 10, 8, 100, null, null
        );
    }
}
