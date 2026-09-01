package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TmdbDiscoverFilterRegistryTest {

    private final TmdbDiscoverFilterRegistry registry = new TmdbDiscoverFilterRegistry();

    @Test
    void containsDefinitionForEveryTypedKey() {
        Stream.of(
                        Arrays.stream(NumericFilterKey.values()),
                        Arrays.stream(DateFilterKey.values()),
                        Arrays.stream(TextFilterKey.values()),
                        Arrays.stream(BooleanFilterKey.values()),
                        Arrays.stream(NamedFilterKey.values()),
                        Arrays.stream(EnumFilterKey.values())
                )
                .flatMap(stream -> stream)
                .forEach(key -> assertNotNull(registry.definition(key), key.name()));
    }

    @Test
    void coversEveryCurrentMovieDiscoverParameter() {
        assertEquals(Set.of(
                "year", "primary_release_year", "vote_average.gte", "vote_average.lte",
                "vote_count.gte", "vote_count.lte", "with_runtime.gte", "with_runtime.lte",
                "page", "primary_release_date.gte", "primary_release_date.lte",
                "release_date.gte", "release_date.lte", "language", "with_original_language",
                "with_origin_country", "region", "watch_region", "certification",
                "certification.gte", "certification.lte", "certification_country",
                "include_adult", "include_video", "with_genres", "without_genres",
                "with_cast", "with_crew", "with_people", "with_companies",
                "without_companies", "with_keywords", "without_keywords",
                "with_watch_providers", "without_watch_providers", "sort_by",
                "with_release_type", "with_watch_monetization_types"
        ), parameters(AiMediaType.MOVIE));
    }

    @Test
    void coversEveryCurrentTvDiscoverParameter() {
        assertEquals(Set.of(
                "first_air_date_year", "vote_average.gte", "vote_average.lte",
                "vote_count.gte", "vote_count.lte", "with_runtime.gte", "with_runtime.lte",
                "page", "first_air_date.gte", "first_air_date.lte",
                "air_date.gte", "air_date.lte", "language", "with_original_language",
                "with_origin_country", "watch_region", "timezone", "include_adult",
                "include_null_first_air_dates", "screened_theatrically", "with_genres",
                "without_genres", "with_companies", "without_companies", "with_keywords",
                "without_keywords", "with_networks", "with_watch_providers",
                "without_watch_providers", "sort_by", "with_watch_monetization_types",
                "with_status", "with_type"
        ), parameters(AiMediaType.SERIES));
    }

    private Set<String> parameters(AiMediaType mediaType) {
        Set<String> parameters = new HashSet<>();
        registry.definitions().values().stream()
                .filter(definition -> definition.supports(mediaType))
                .forEach(definition -> {
                    if (definition.parameters().get(mediaType) != null) {
                        parameters.addAll(definition.parameters().get(mediaType).values());
                    }
                    String excluded = definition.excludeParameters().get(mediaType);
                    if (excluded != null) {
                        parameters.add(excluded);
                    }
                });
        return parameters;
    }
}
