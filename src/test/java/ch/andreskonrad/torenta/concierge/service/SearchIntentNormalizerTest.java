package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchIntentNormalizerTest {

    private final SearchIntentNormalizer normalizer =
            new SearchIntentNormalizer(new TmdbDiscoverFilterRegistry());

    @Test
    void omitsZeroPlaceholdersWithoutEvidenceAndAcceptsCorrectedName() {
        SearchIntent extracted = intent(
                AiMediaType.MOVIE,
                List.of(
                        new NumericFilterCriterion(NumericFilterKey.YEAR, FilterOperator.GTE, 0.0, ""),
                        new NumericFilterCriterion(NumericFilterKey.YEAR, FilterOperator.LTE, 0.0, null)
                ),
                List.of(),
                List.of(),
                List.of(new NamedFilterCriterion(
                        NamedFilterKey.CAST, List.of("Jack Nicholson"),
                        FilterPolarity.INCLUDE, FilterMatch.ANY, "jack nickolson"
                )),
                List.of()
        );

        SearchIntent normalized = normalizer.normalize(
                extracted, "a funny movie with jack nickolson"
        );

        assertTrue(normalized.numericFilters().isEmpty());
        assertEquals("Jack Nicholson", normalized.namedFilters().getFirst().names().getFirst());
    }

    @Test
    void validatesCodesDatesRangesDependenciesAndMediaSupportByOmission() {
        SearchIntent extracted = intent(
                AiMediaType.SERIES,
                List.of(
                        numeric(NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 8, "above 8"),
                        numeric(NumericFilterKey.VOTE_AVERAGE, FilterOperator.LTE, 7, "below 7"),
                        numeric(NumericFilterKey.PRIMARY_RELEASE_YEAR, FilterOperator.EQ, 2020, "2020")
                ),
                List.of(
                        date(DateFilterKey.FIRST_AIR_DATE, FilterOperator.GTE,
                                "not-a-date", "after yesterday"),
                        date(DateFilterKey.AIR_DATE, FilterOperator.LTE,
                                "2025-12-31", "before 2026")
                ),
                List.of(
                        text(TextFilterKey.ORIGINAL_LANGUAGE, "EN", "English"),
                        text(TextFilterKey.WATCH_REGION, "us", "US services"),
                        text(TextFilterKey.CERTIFICATION, "PG-13", "PG-13")
                ),
                List.of(
                        named(NamedFilterKey.WATCH_PROVIDER, "Netflix", "Netflix"),
                        named(NamedFilterKey.CAST, "Actor Name", "Actor Name")
                ),
                List.of(new EnumFilterCriterion(
                        EnumFilterKey.WATCH_MONETIZATION_TYPE,
                        List.of("FLATRATE", "NOT_REAL"), FilterMatch.ANY, "streaming"
                ))
        );

        SearchIntent normalized = normalizer.normalize(
                extracted,
                "English series above 8 below 7 in 2020 after yesterday before 2026 "
                        + "on Netflix streaming via US services, PG-13 with Actor Name"
        );

        assertTrue(normalized.numericFilters().isEmpty());
        assertEquals(List.of("2025-12-31"),
                normalized.dateFilters().stream().map(DateFilterCriterion::value).toList());
        assertEquals(
                List.of(TextFilterKey.ORIGINAL_LANGUAGE, TextFilterKey.WATCH_REGION),
                normalized.textFilters().stream().map(TextFilterCriterion::key).toList()
        );
        assertEquals(List.of(NamedFilterKey.WATCH_PROVIDER, NamedFilterKey.CAST),
                normalized.namedFilters().stream().map(NamedFilterCriterion::key).toList());
        assertEquals(List.of("FLATRATE"), normalized.enumFilters().getFirst().values());
    }

    @Test
    void retainsDependencyBoundFiltersForFactualRankingFallback() {
        SearchIntent extracted = intent(
                AiMediaType.MOVIE,
                List.of(), List.of(),
                List.of(text(TextFilterKey.CERTIFICATION, "PG", "rated PG")),
                List.of(named(NamedFilterKey.WATCH_PROVIDER, "Netflix", "Netflix")),
                List.of(new EnumFilterCriterion(
                        EnumFilterKey.WATCH_MONETIZATION_TYPE,
                        List.of("BUY"), FilterMatch.ANY, "buy"
                ))
        );

        SearchIntent normalized = normalizer.normalize(
                extracted, "rated PG on Netflix to buy"
        );

        assertEquals(List.of(TextFilterKey.CERTIFICATION),
                normalized.textFilters().stream().map(TextFilterCriterion::key).toList());
        assertEquals(List.of(NamedFilterKey.WATCH_PROVIDER),
                normalized.namedFilters().stream().map(NamedFilterCriterion::key).toList());
        assertEquals(List.of(EnumFilterKey.WATCH_MONETIZATION_TYPE),
                normalized.enumFilters().stream().map(EnumFilterCriterion::key).toList());
    }

    @Test
    void omitsMalformedTimezoneWithoutFailingTheRequest() {
        SearchIntent extracted = intent(
                AiMediaType.SERIES, List.of(), List.of(),
                List.of(text(TextFilterKey.TIMEZONE, "Pacific Time", "Pacific Time")),
                List.of(), List.of()
        );

        SearchIntent normalized = normalizer.normalize(
                extracted, "a series in Pacific Time"
        );

        assertTrue(normalized.textFilters().isEmpty());
    }

    @Test
    void keepsOnlyFirstSortByValue() {
        SearchIntent extracted = intent(
                AiMediaType.MOVIE, List.of(), List.of(), List.of(), List.of(),
                List.of(new EnumFilterCriterion(
                        EnumFilterKey.SORT_BY,
                        List.of("POPULARITY_DESC", "VOTE_AVERAGE_DESC"),
                        FilterMatch.ANY,
                        "popular and highest rated"
                ))
        );

        SearchIntent normalized = normalizer.normalize(
                extracted, "popular and highest rated movies"
        );

        assertEquals(1, normalized.enumFilters().size());
        assertEquals(List.of("POPULARITY_DESC"), normalized.enumFilters().getFirst().values());
    }

    @Test
    void rejectsMissingIntentAndNormalizesNullableTopLevelValues() {
        assertThrows(IllegalStateException.class, () -> normalizer.normalize(null, "request"));

        SearchIntent normalized = normalizer.normalize(
                new SearchIntent(null, null, "The Matrix", null, null, null, null, null, null),
                null
        );

        assertEquals(AiMediaType.ANY, normalized.mediaType());
        assertTrue(normalized.moods().isEmpty());
        assertNull(normalized.similarTo());
        assertTrue(normalized.numericFilters().isEmpty());
        assertTrue(normalized.dateFilters().isEmpty());
        assertTrue(normalized.textFilters().isEmpty());
        assertTrue(normalized.booleanFilters().isEmpty());
        assertTrue(normalized.namedFilters().isEmpty());
        assertTrue(normalized.enumFilters().isEmpty());
    }

    @Test
    void validatesNumericAndDateCriteriaAndRemovesDuplicatesAndContradictions() {
        List<NumericFilterCriterion> numeric = new ArrayList<>(Arrays.asList(
                null,
                numeric(null, FilterOperator.EQ, 2020, "request"),
                numeric(NumericFilterKey.YEAR, null, 2020, "request"),
                new NumericFilterCriterion(NumericFilterKey.YEAR, FilterOperator.EQ, null, "request"),
                numeric(NumericFilterKey.YEAR, FilterOperator.EQ, Double.NaN, "request"),
                numeric(NumericFilterKey.YEAR, FilterOperator.EQ, 2020, "absent"),
                numeric(NumericFilterKey.FIRST_AIR_DATE_YEAR, FilterOperator.EQ, 2020, "request"),
                numeric(NumericFilterKey.YEAR, FilterOperator.GTE, 2020, "request"),
                numeric(NumericFilterKey.YEAR, FilterOperator.EQ, 999, "request"),
                numeric(NumericFilterKey.YEAR, FilterOperator.EQ, 10000, "request"),
                numeric(NumericFilterKey.YEAR, FilterOperator.EQ, 2020.5, "request"),
                numeric(NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 8, "request"),
                numeric(NumericFilterKey.VOTE_AVERAGE, FilterOperator.GTE, 7, "request"),
                numeric(NumericFilterKey.VOTE_AVERAGE, FilterOperator.LTE, 6, "request"),
                numeric(NumericFilterKey.PAGE, FilterOperator.EQ, 2, "request")
        ));
        List<DateFilterCriterion> dates = new ArrayList<>(Arrays.asList(
                null,
                date(null, FilterOperator.GTE, "2020-01-01", "request"),
                date(DateFilterKey.PRIMARY_RELEASE_DATE, null, "2020-01-01", "request"),
                new DateFilterCriterion(
                        DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.GTE, null, "request"
                ),
                date(DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.GTE, "2020-01-01", "absent"),
                date(DateFilterKey.FIRST_AIR_DATE, FilterOperator.GTE, "2020-01-01", "request"),
                date(DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.EQ, "2020-01-01", "request"),
                date(DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.GTE, "2025-01-01", "request"),
                date(DateFilterKey.PRIMARY_RELEASE_DATE, FilterOperator.LTE, "2020-01-01", "request"),
                date(DateFilterKey.RELEASE_DATE, FilterOperator.GTE, "not-a-date", "request"),
                date(DateFilterKey.RELEASE_DATE, FilterOperator.GTE, "2020-01-01", "request"),
                date(DateFilterKey.RELEASE_DATE, FilterOperator.GTE, "2021-01-01", "request")
        ));

        SearchIntent normalized = normalizer.normalize(
                intent(AiMediaType.MOVIE, numeric, dates, List.of(), List.of(), List.of()),
                "request"
        );

        assertEquals(List.of(NumericFilterKey.PAGE),
                normalized.numericFilters().stream().map(NumericFilterCriterion::key).toList());
        assertEquals(List.of(DateFilterKey.RELEASE_DATE),
                normalized.dateFilters().stream().map(DateFilterCriterion::key).toList());
    }

    @Test
    void normalizesTextBooleanAndNamedCriteriaWithDefaults() {
        List<TextFilterCriterion> text = new ArrayList<>(Arrays.asList(
                null,
                text(null, "en", "request"),
                text(TextFilterKey.LANGUAGE, null, "request"),
                text(TextFilterKey.LANGUAGE, "en-us", "absent"),
                text(TextFilterKey.LANGUAGE, "en-us", "request"),
                text(TextFilterKey.ORIGIN_COUNTRY, "ch", "request"),
                text(TextFilterKey.CERTIFICATION, "pg-13", "request"),
                text(TextFilterKey.TIMEZONE, "Europe/Zurich", "request"),
                text(TextFilterKey.WATCH_REGION, "toolong".repeat(40), "request"),
                text(TextFilterKey.ORIGINAL_LANGUAGE, "not_valid!", "request"),
                text(TextFilterKey.LANGUAGE, "de", "request")
        ));
        List<BooleanFilterCriterion> booleans = new ArrayList<>(Arrays.asList(
                null,
                new BooleanFilterCriterion(null, true, "request"),
                new BooleanFilterCriterion(BooleanFilterKey.INCLUDE_ADULT, null, "request"),
                new BooleanFilterCriterion(BooleanFilterKey.INCLUDE_ADULT, true, "absent"),
                new BooleanFilterCriterion(BooleanFilterKey.INCLUDE_NULL_FIRST_AIR_DATES, true, "request"),
                new BooleanFilterCriterion(BooleanFilterKey.INCLUDE_ADULT, true, "request"),
                new BooleanFilterCriterion(BooleanFilterKey.INCLUDE_ADULT, false, "request")
        ));
        List<NamedFilterCriterion> named = new ArrayList<>(Arrays.asList(
                null,
                new NamedFilterCriterion(null, List.of("name"), null, null, "request"),
                new NamedFilterCriterion(
                        NamedFilterKey.CAST, Arrays.asList(null, " Actor ", "", "Actor"),
                        null, null, "request"
                ),
                new NamedFilterCriterion(
                        NamedFilterKey.NETWORK, List.of("HBO"), FilterPolarity.INCLUDE,
                        FilterMatch.ALL, "request"
                ),
                new NamedFilterCriterion(
                        NamedFilterKey.CAST, List.of("Other"), FilterPolarity.INCLUDE,
                        FilterMatch.ALL, "request"
                ),
                new NamedFilterCriterion(
                        NamedFilterKey.CAST, List.of("Actor"), FilterPolarity.EXCLUDE,
                        FilterMatch.ALL, "request"
                )
        ));
        SearchIntent extracted = new SearchIntent(
                AiMediaType.MOVIE, List.of(), null, List.of(), List.of(), text,
                booleans, named, List.of()
        );

        SearchIntent normalized = normalizer.normalize(extracted, "request");

        assertEquals(List.of("en-US", "CH", "PG-13"),
                normalized.textFilters().stream().map(TextFilterCriterion::value).toList());
        assertEquals(List.of(BooleanFilterKey.INCLUDE_ADULT),
                normalized.booleanFilters().stream().map(BooleanFilterCriterion::key).toList());
        assertEquals(FilterPolarity.INCLUDE, normalized.namedFilters().getFirst().polarity());
        assertEquals(FilterMatch.ANY, normalized.namedFilters().getFirst().matching());
        assertEquals(List.of("Actor"), normalized.namedFilters().getFirst().names());
    }

    @Test
    void normalizesEnumValuesAndAcceptsOnlyEvidencedSimilarTitle() {
        List<EnumFilterCriterion> enums = new ArrayList<>(Arrays.asList(
                null,
                new EnumFilterCriterion(null, List.of("BUY"), null, "request"),
                new EnumFilterCriterion(EnumFilterKey.TV_TYPE, null, null, "request"),
                new EnumFilterCriterion(
                        EnumFilterKey.TV_TYPE, List.of("SCRIPTED"), null, "absent"
                ),
                new EnumFilterCriterion(
                        EnumFilterKey.SORT_BY,
                        List.of("vote-average-desc", "NOT_REAL"), null, "request"
                ),
                new EnumFilterCriterion(
                        EnumFilterKey.WATCH_MONETIZATION_TYPE,
                        List.of("buy", "rent", "buy"), null, "request"
                )
        ));
        SearchIntent extracted = new SearchIntent(
                AiMediaType.MOVIE, List.of(" dark ", "", "dark"), "The Matrix",
                List.of(), List.of(), List.of(), List.of(), List.of(), enums
        );

        SearchIntent accepted = normalizer.normalize(extracted, "something similar to The Matrix request");
        SearchIntent rejected = normalizer.normalize(extracted, "request");

        assertEquals("The Matrix", accepted.similarTo());
        assertNull(rejected.similarTo());
        assertEquals(List.of("DARK"), accepted.moods());
        assertEquals(List.of("VOTE_AVERAGE_DESC"), accepted.enumFilters().getFirst().values());
        assertEquals(List.of("BUY", "RENT"), accepted.enumFilters().get(1).values());
        assertEquals(FilterMatch.ANY, accepted.enumFilters().getFirst().matching());
    }

    private SearchIntent intent(
            AiMediaType type,
            List<NumericFilterCriterion> numeric,
            List<DateFilterCriterion> dates,
            List<TextFilterCriterion> text,
            List<NamedFilterCriterion> named,
            List<EnumFilterCriterion> enums
    ) {
        return new SearchIntent(
                type, List.of(), null, numeric, dates, text,
                List.of(), named, enums
        );
    }

    private NumericFilterCriterion numeric(
            NumericFilterKey key,
            FilterOperator operator,
            double value,
            String evidence
    ) {
        return new NumericFilterCriterion(key, operator, value, evidence);
    }

    private DateFilterCriterion date(
            DateFilterKey key,
            FilterOperator operator,
            String value,
            String evidence
    ) {
        return new DateFilterCriterion(key, operator, value, evidence);
    }

    private TextFilterCriterion text(TextFilterKey key, String value, String evidence) {
        return new TextFilterCriterion(key, value, evidence);
    }

    private NamedFilterCriterion named(NamedFilterKey key, String name, String evidence) {
        return new NamedFilterCriterion(
                key, List.of(name), FilterPolarity.INCLUDE, FilterMatch.ANY, evidence
        );
    }
}
