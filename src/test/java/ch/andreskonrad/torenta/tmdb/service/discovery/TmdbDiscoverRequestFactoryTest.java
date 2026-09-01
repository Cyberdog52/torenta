package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class TmdbDiscoverRequestFactoryTest {

    @Test
    void rendersOnlyRegistryParametersWithDefaultsOverridesAndRankingFallback() {
        TmdbNameResolver resolver = mock(TmdbNameResolver.class);
        when(resolver.resolve(
                eq(NamedFilterKey.COMPANY), eq("Resolved"), anyString(),
                eq(AiMediaType.MOVIE), eq("US")
        )).thenReturn(OptionalInt.of(10));
        when(resolver.resolve(
                eq(NamedFilterKey.COMPANY), eq("Missing"), anyString(),
                eq(AiMediaType.MOVIE), eq("US")
        )).thenReturn(OptionalInt.empty());
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), null,
                List.of(new NumericFilterCriterion(
                        NumericFilterKey.PAGE, FilterOperator.EQ, 2.0, "page 2"
                )),
                List.of(),
                List.of(new TextFilterCriterion(
                        TextFilterKey.WATCH_REGION, "US", "US"
                )),
                List.of(new BooleanFilterCriterion(
                        BooleanFilterKey.INCLUDE_ADULT, true, "adult"
                )),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.COMPANY, List.of("Resolved", "Missing"),
                        FilterPolarity.INCLUDE, FilterMatch.ALL, "Resolved Missing"
                )),
                List.of(new EnumFilterCriterion(
                        EnumFilterKey.SORT_BY, List.of("VOTE_AVERAGE_DESC"),
                        FilterMatch.ANY, "highest rated"
                ))
        );
        TmdbDiscoverRequest request = new TmdbDiscoverRequestFactory(
                new TmdbDiscoverFilterRegistry(), resolver
        ).create(intent, AiMediaType.MOVIE);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                "https://example.test/3/discover/movie?language=en-US"
        );

        request.applyTo(builder);
        URI uri = builder.build().encode().toUri();
        String query = uri.getRawQuery();

        assertEquals(1, request.rankingOnlyCriteria().size());
        assertEquals(List.of("Missing"), request.rankingOnlyCriteria().getFirst().names());
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("page=2"));
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("sort_by=vote_average.desc"));
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("include_adult=true"));
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("include_video=false"));
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("with_companies=10"));
        assertFalse(query.contains("Missing"));
    }

    @Test
    void omitsUnsatisfiedDependenciesFromTmdbAndKeepsNamedFilterForRanking() {
        TmdbNameResolver resolver = mock(TmdbNameResolver.class);
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), null, List.of(), List.of(),
                List.of(new TextFilterCriterion(
                        TextFilterKey.CERTIFICATION, "PG", "rated PG"
                )),
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.WATCH_PROVIDER, List.of("Netflix"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "Netflix"
                )),
                List.of(new EnumFilterCriterion(
                        EnumFilterKey.WATCH_MONETIZATION_TYPE,
                        List.of("FLATRATE"), FilterMatch.ANY, "streaming"
                ))
        );

        TmdbDiscoverRequest request = new TmdbDiscoverRequestFactory(
                new TmdbDiscoverFilterRegistry(), resolver
        ).create(intent, AiMediaType.MOVIE);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                "https://example.test/3/discover/movie?language=en-US"
        );
        request.applyTo(builder);
        String query = builder.build().encode().toUri().getRawQuery();

        assertFalse(query.contains("certification="));
        assertFalse(query.contains("with_watch_providers"));
        assertFalse(query.contains("with_watch_monetization_types"));
        assertEquals(List.of("Netflix"), request.rankingOnlyCriteria().getFirst().names());
        verifyNoInteractions(resolver);
    }

    @Test
    void keepsUnresolvedWatchProviderOnlyForRankingAndOutOfDiscoverQuery() {
        TmdbNameResolver resolver = mock(TmdbNameResolver.class);
        when(resolver.resolve(
                eq(NamedFilterKey.WATCH_PROVIDER), eq("Amazon"), anyString(),
                eq(AiMediaType.MOVIE), eq("US")
        )).thenReturn(OptionalInt.empty());
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), null, List.of(), List.of(),
                List.of(new TextFilterCriterion(
                        TextFilterKey.WATCH_REGION, "US", "US"
                )),
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.WATCH_PROVIDER, List.of("Amazon"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "Amazon"
                )),
                List.of()
        );

        TmdbDiscoverRequest request = new TmdbDiscoverRequestFactory(
                new TmdbDiscoverFilterRegistry(), resolver
        ).create(intent, AiMediaType.MOVIE);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                "https://example.test/3/discover/movie?language=en-US"
        );

        request.applyTo(builder);
        String query = builder.build().encode().toUri().getRawQuery();

        assertFalse(query.contains("with_watch_providers"));
        assertEquals(1, request.rankingOnlyCriteria().size());
        assertEquals(List.of("Amazon"), request.rankingOnlyCriteria().getFirst().names());
    }

    @Test
    void omitsMovieOnlySortFromSeriesDiscover() {
        SearchIntent intent = new SearchIntent(
                AiMediaType.SERIES, List.of(), null, List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(new EnumFilterCriterion(
                        EnumFilterKey.SORT_BY, List.of("ORIGINAL_TITLE_DESC"),
                        FilterMatch.ANY, "original title descending"
                ))
        );
        TmdbDiscoverRequest request = new TmdbDiscoverRequestFactory(
                new TmdbDiscoverFilterRegistry(), mock(TmdbNameResolver.class)
        ).create(intent, AiMediaType.SERIES);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                "https://example.test/3/discover/tv?language=en-US"
        );

        request.applyTo(builder);

        assertFalse(builder.build().encode().toUri().getRawQuery()
                .contains("original_title"));
    }

    @Test
    void rendersAllFilterFamiliesWithDependenciesPolarityAndDecimalValues() {
        TmdbNameResolver resolver = mock(TmdbNameResolver.class);
        when(resolver.resolve(
                eq(NamedFilterKey.GENRE), eq("Comedy"), anyString(),
                eq(AiMediaType.MOVIE), eq("CH")
        )).thenReturn(OptionalInt.of(35));
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of(), null,
                List.of(new NumericFilterCriterion(
                        NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 7.5, "rating"
                )),
                List.of(new DateFilterCriterion(
                        DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.LTE,
                        "2025-12-31", "date"
                )),
                List.of(
                        new TextFilterCriterion(TextFilterKey.REGION, "CH", "region"),
                        new TextFilterCriterion(TextFilterKey.WATCH_REGION, "CH", "watch"),
                        new TextFilterCriterion(TextFilterKey.CERTIFICATION, "12", "certificate")
                ),
                List.of(new BooleanFilterCriterion(
                        BooleanFilterKey.INCLUDE_ADULT, true, "adult"
                )),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.GENRE, List.of("Comedy"), FilterPolarity.EXCLUDE,
                        FilterMatch.ANY, "not comedy"
                )),
                List.of(
                        new EnumFilterCriterion(
                                EnumFilterKey.RELEASE_TYPE,
                                List.of("THEATRICAL", "DIGITAL"), FilterMatch.ALL, "release"
                        ),
                        new EnumFilterCriterion(
                                EnumFilterKey.WATCH_MONETIZATION_TYPE,
                                List.of("RENT", "BUY"), FilterMatch.ANY, "availability"
                        )
                )
        );
        TmdbDiscoverRequest request = new TmdbDiscoverRequestFactory(
                new TmdbDiscoverFilterRegistry(), resolver
        ).create(intent, AiMediaType.MOVIE);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                "https://example.test/discover"
        );

        request.applyTo(builder);
        String query = builder.build().encode().toUri().getRawQuery();

        assertTrue(query.contains("vote_average.gte=7.5"));
        assertTrue(query.contains("primary_release_date.lte=2025-12-31"));
        assertTrue(query.contains("certification=12"));
        assertTrue(query.contains("include_adult=true"));
        assertTrue(query.contains("without_genres=35"));
        assertTrue(query.contains("with_release_type=3,4"));
        assertTrue(query.contains("with_watch_monetization_types=rent%7Cbuy"));
        assertTrue(request.rankingOnlyCriteria().isEmpty());
    }
}
