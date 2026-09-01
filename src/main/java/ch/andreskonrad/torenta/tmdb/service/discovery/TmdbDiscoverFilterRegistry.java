package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterDefinition.Dependency;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterDefinition.ValueKind;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.Year;

@Component
public class TmdbDiscoverFilterRegistry {

    private static final Set<AiMediaType> BOTH = Set.of(AiMediaType.MOVIE, AiMediaType.SERIES);
    private static final Set<FilterPolarity> INCLUDE =
            Set.of(FilterPolarity.INCLUDE);
    private static final Set<FilterPolarity> INCLUDE_EXCLUDE =
            Set.of(FilterPolarity.INCLUDE, FilterPolarity.EXCLUDE);

    private final Map<Enum<?>, TmdbDiscoverFilterDefinition> definitions;

    public TmdbDiscoverFilterRegistry() {
        Map<Enum<?>, TmdbDiscoverFilterDefinition> mutable = new LinkedHashMap<>();

        double maximumYear = Year.now().getValue() + 5;
        numeric(mutable, NumericFilterKey.YEAR, Set.of(AiMediaType.MOVIE),
                eq("year", null), 1870, maximumYear);
        numeric(mutable, NumericFilterKey.PRIMARY_RELEASE_YEAR, Set.of(AiMediaType.MOVIE),
                eq("primary_release_year", null), 1870, maximumYear);
        numeric(mutable, NumericFilterKey.FIRST_AIR_DATE_YEAR, Set.of(AiMediaType.SERIES),
                eq(null, "first_air_date_year"), 1870, maximumYear);
        numeric(mutable, NumericFilterKey.VOTE_AVERAGE, BOTH,
                bounds("vote_average.gte", "vote_average.lte"), 0, 10);
        numeric(mutable, NumericFilterKey.VOTE_COUNT, BOTH,
                bounds("vote_count.gte", "vote_count.lte"), 0, 10_000_000);
        numeric(mutable, NumericFilterKey.RUNTIME, BOTH,
                bounds("with_runtime.gte", "with_runtime.lte"), 1, 1440);
        numeric(mutable, NumericFilterKey.PAGE, BOTH, eq("page", "page"), 1, 500);

        date(mutable, DateFilterKey.PRIMARY_RELEASE_DATE, Set.of(AiMediaType.MOVIE),
                bounds("primary_release_date.gte", "primary_release_date.lte"));
        date(mutable, DateFilterKey.RELEASE_DATE, Set.of(AiMediaType.MOVIE),
                bounds("release_date.gte", "release_date.lte"));
        date(mutable, DateFilterKey.FIRST_AIR_DATE, Set.of(AiMediaType.SERIES),
                bounds("first_air_date.gte", "first_air_date.lte"));
        date(mutable, DateFilterKey.AIR_DATE, Set.of(AiMediaType.SERIES),
                bounds("air_date.gte", "air_date.lte"));

        text(mutable, TextFilterKey.LANGUAGE, BOTH, "language", "language",
                "[a-z]{2}(?:-[A-Z]{2})?", Dependency.NONE);
        text(mutable, TextFilterKey.ORIGINAL_LANGUAGE, BOTH,
                "with_original_language", "with_original_language", "[a-z]{2}", Dependency.NONE);
        text(mutable, TextFilterKey.ORIGIN_COUNTRY, BOTH,
                "with_origin_country", "with_origin_country", "[A-Z]{2}", Dependency.NONE);
        text(mutable, TextFilterKey.REGION, Set.of(AiMediaType.MOVIE),
                "region", null, "[A-Z]{2}", Dependency.NONE);
        text(mutable, TextFilterKey.WATCH_REGION, BOTH,
                "watch_region", "watch_region", "[A-Z]{2}", Dependency.NONE);
        text(mutable, TextFilterKey.TIMEZONE, Set.of(AiMediaType.SERIES),
                null, "timezone", ".{1,100}", Dependency.NONE);
        text(mutable, TextFilterKey.CERTIFICATION, Set.of(AiMediaType.MOVIE),
                "certification", null, ".{1,20}", Dependency.CERTIFICATION_TERRITORY);
        text(mutable, TextFilterKey.CERTIFICATION_GTE, Set.of(AiMediaType.MOVIE),
                "certification.gte", null, ".{1,20}", Dependency.CERTIFICATION_TERRITORY);
        text(mutable, TextFilterKey.CERTIFICATION_LTE, Set.of(AiMediaType.MOVIE),
                "certification.lte", null, ".{1,20}", Dependency.CERTIFICATION_TERRITORY);
        text(mutable, TextFilterKey.CERTIFICATION_COUNTRY, Set.of(AiMediaType.MOVIE),
                "certification_country", null, "[A-Z]{2}", Dependency.NONE);

        bool(mutable, BooleanFilterKey.INCLUDE_ADULT, BOTH, "include_adult", "include_adult");
        bool(mutable, BooleanFilterKey.INCLUDE_VIDEO, Set.of(AiMediaType.MOVIE),
                "include_video", null);
        bool(mutable, BooleanFilterKey.INCLUDE_NULL_FIRST_AIR_DATES, Set.of(AiMediaType.SERIES),
                null, "include_null_first_air_dates");
        bool(mutable, BooleanFilterKey.SCREENED_THEATRICALLY, Set.of(AiMediaType.SERIES),
                null, "screened_theatrically");

        named(mutable, NamedFilterKey.GENRE, BOTH, "with_genres", "with_genres",
                "without_genres", "without_genres", INCLUDE_EXCLUDE, Dependency.NONE);
        named(mutable, NamedFilterKey.CAST, Set.of(AiMediaType.MOVIE), "with_cast", null,
                null, null, INCLUDE, Dependency.NONE);
        named(mutable, NamedFilterKey.CREW, Set.of(AiMediaType.MOVIE), "with_crew", null,
                null, null, INCLUDE, Dependency.NONE);
        named(mutable, NamedFilterKey.PEOPLE, Set.of(AiMediaType.MOVIE), "with_people", null,
                null, null, INCLUDE, Dependency.NONE);
        named(mutable, NamedFilterKey.COMPANY, BOTH, "with_companies", "with_companies",
                "without_companies", "without_companies", INCLUDE_EXCLUDE, Dependency.NONE);
        named(mutable, NamedFilterKey.KEYWORD, BOTH, "with_keywords", "with_keywords",
                "without_keywords", "without_keywords", INCLUDE_EXCLUDE, Dependency.NONE);
        named(mutable, NamedFilterKey.NETWORK, Set.of(AiMediaType.SERIES), null, "with_networks",
                null, null, INCLUDE, Dependency.NONE);
        named(mutable, NamedFilterKey.WATCH_PROVIDER, BOTH,
                "with_watch_providers", "with_watch_providers",
                "without_watch_providers", "without_watch_providers",
                INCLUDE_EXCLUDE, Dependency.WATCH_REGION);

        enumFilter(mutable, EnumFilterKey.SORT_BY, BOTH, "sort_by", "sort_by", sortValues());
        enumFilter(mutable, EnumFilterKey.RELEASE_TYPE, Set.of(AiMediaType.MOVIE),
                "with_release_type", null, Map.of(
                        "PREMIERE", "1", "LIMITED_THEATRICAL", "2", "THEATRICAL", "3",
                        "DIGITAL", "4", "PHYSICAL", "5", "TV", "6"
                ));
        enumFilter(mutable, EnumFilterKey.WATCH_MONETIZATION_TYPE, BOTH,
                "with_watch_monetization_types", "with_watch_monetization_types", Map.of(
                        "FLATRATE", "flatrate", "FREE", "free", "ADS", "ads",
                        "RENT", "rent", "BUY", "buy"
                ), Dependency.WATCH_REGION);
        enumFilter(mutable, EnumFilterKey.TV_STATUS, Set.of(AiMediaType.SERIES),
                null, "with_status", Map.of(
                        "RETURNING_SERIES", "0", "PLANNED", "1", "IN_PRODUCTION", "2",
                        "ENDED", "3", "CANCELED", "4", "PILOT", "5"
                ));
        enumFilter(mutable, EnumFilterKey.TV_TYPE, Set.of(AiMediaType.SERIES),
                null, "with_type", Map.of(
                        "DOCUMENTARY", "0", "NEWS", "1", "MINISERIES", "2",
                        "REALITY", "3", "SCRIPTED", "4", "TALK_SHOW", "5",
                        "VIDEO", "6"
                ));

        definitions = Map.copyOf(mutable);
    }

    public TmdbDiscoverFilterDefinition definition(Enum<?> key) {
        return key == null ? null : definitions.get(key);
    }

    public Map<Enum<?>, TmdbDiscoverFilterDefinition> definitions() {
        return definitions;
    }

    private void numeric(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            NumericFilterKey key,
            Set<AiMediaType> media,
            Map<AiMediaType, Map<FilterOperator, String>> parameters,
            double minimum,
            double maximum
    ) {
        put(target, key, ValueKind.NUMERIC, media, operators(parameters), parameters,
                Map.of(), INCLUDE, minimum, maximum, null, Map.of(), Dependency.NONE);
    }

    private void date(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            DateFilterKey key,
            Set<AiMediaType> media,
            Map<AiMediaType, Map<FilterOperator, String>> parameters
    ) {
        put(target, key, ValueKind.DATE, media, operators(parameters), parameters,
                Map.of(), INCLUDE, null, null, null, Map.of(), Dependency.NONE);
    }

    private void text(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            TextFilterKey key,
            Set<AiMediaType> media,
            String movieParameter,
            String seriesParameter,
            String pattern,
            Dependency dependency
    ) {
        put(target, key, ValueKind.TEXT, media, Set.of(FilterOperator.EQ),
                eq(movieParameter, seriesParameter), Map.of(), INCLUDE,
                null, null, pattern, Map.of(), dependency);
    }

    private void bool(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            BooleanFilterKey key,
            Set<AiMediaType> media,
            String movieParameter,
            String seriesParameter
    ) {
        put(target, key, ValueKind.BOOLEAN, media, Set.of(FilterOperator.EQ),
                eq(movieParameter, seriesParameter), Map.of(), INCLUDE,
                null, null, null, Map.of(), Dependency.NONE);
    }

    private void named(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            NamedFilterKey key,
            Set<AiMediaType> media,
            String movieParameter,
            String seriesParameter,
            String movieExcludeParameter,
            String seriesExcludeParameter,
            Set<FilterPolarity> polarities,
            Dependency dependency
    ) {
        put(target, key, ValueKind.NAMED, media, Set.of(FilterOperator.EQ),
                eq(movieParameter, seriesParameter), mediaMap(movieExcludeParameter, seriesExcludeParameter),
                polarities, null, null, null, Map.of(), dependency);
    }

    private void enumFilter(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            EnumFilterKey key,
            Set<AiMediaType> media,
            String movieParameter,
            String seriesParameter,
            Map<String, String> values
    ) {
        enumFilter(target, key, media, movieParameter, seriesParameter, values, Dependency.NONE);
    }

    private void enumFilter(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            EnumFilterKey key,
            Set<AiMediaType> media,
            String movieParameter,
            String seriesParameter,
            Map<String, String> values,
            Dependency dependency
    ) {
        put(target, key, ValueKind.ENUM, media, Set.of(FilterOperator.EQ),
                eq(movieParameter, seriesParameter), Map.of(), INCLUDE,
                null, null, null, values, dependency);
    }

    private void put(
            Map<Enum<?>, TmdbDiscoverFilterDefinition> target,
            Enum<?> key,
            ValueKind kind,
            Set<AiMediaType> media,
            Set<FilterOperator> operators,
            Map<AiMediaType, Map<FilterOperator, String>> parameters,
            Map<AiMediaType, String> excludes,
            Set<FilterPolarity> polarities,
            Double minimum,
            Double maximum,
            String pattern,
            Map<String, String> values,
            Dependency dependency
    ) {
        target.put(key, new TmdbDiscoverFilterDefinition(
                key, kind, media, operators, parameters, excludes, polarities,
                minimum, maximum, pattern, values, dependency
        ));
    }

    private static Map<AiMediaType, Map<FilterOperator, String>> eq(
            String movie,
            String series
    ) {
        Map<AiMediaType, Map<FilterOperator, String>> result = new LinkedHashMap<>();
        if (movie != null) {
            result.put(AiMediaType.MOVIE, Map.of(FilterOperator.EQ, movie));
        }
        if (series != null) {
            result.put(AiMediaType.SERIES, Map.of(FilterOperator.EQ, series));
        }
        return Map.copyOf(result);
    }

    private static Map<AiMediaType, Map<FilterOperator, String>> bounds(
            String gte,
            String lte
    ) {
        return Map.of(
                AiMediaType.MOVIE, Map.of(FilterOperator.GTE, gte, FilterOperator.LTE, lte),
                AiMediaType.SERIES, Map.of(FilterOperator.GTE, gte, FilterOperator.LTE, lte)
        );
    }

    private static Map<AiMediaType, String> mediaMap(String movie, String series) {
        Map<AiMediaType, String> result = new LinkedHashMap<>();
        if (movie != null) {
            result.put(AiMediaType.MOVIE, movie);
        }
        if (series != null) {
            result.put(AiMediaType.SERIES, series);
        }
        return Map.copyOf(result);
    }

    private static Set<FilterOperator> operators(
            Map<AiMediaType, Map<FilterOperator, String>> parameters
    ) {
        EnumSet<FilterOperator> operators = EnumSet.noneOf(FilterOperator.class);
        parameters.values().forEach(values -> operators.addAll(values.keySet()));
        return Set.copyOf(operators);
    }

    private static Map<String, String> sortValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String value : Set.of(
                "POPULARITY_ASC", "POPULARITY_DESC", "VOTE_AVERAGE_ASC", "VOTE_AVERAGE_DESC",
                "VOTE_COUNT_ASC", "VOTE_COUNT_DESC", "PRIMARY_RELEASE_DATE_ASC",
                "PRIMARY_RELEASE_DATE_DESC", "TITLE_ASC", "TITLE_DESC", "REVENUE_ASC",
                "REVENUE_DESC", "ORIGINAL_TITLE_ASC", "ORIGINAL_TITLE_DESC",
                "FIRST_AIR_DATE_ASC", "FIRST_AIR_DATE_DESC", "NAME_ASC", "NAME_DESC",
                "ORIGINAL_NAME_ASC", "ORIGINAL_NAME_DESC"
        )) {
            int separator = value.lastIndexOf('_');
            values.put(
                    value,
                    value.substring(0, separator).toLowerCase(Locale.ROOT)
                            + "." + value.substring(separator + 1).toLowerCase(Locale.ROOT)
            );
        }
        return Map.copyOf(values);
    }
}
