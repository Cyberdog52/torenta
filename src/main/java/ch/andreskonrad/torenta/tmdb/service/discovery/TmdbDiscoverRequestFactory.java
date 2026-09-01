package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequest.RenderedCriterion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.Collectors;

@Component
public class TmdbDiscoverRequestFactory {

    private final TmdbDiscoverFilterRegistry registry;
    private final TmdbNameResolver nameResolver;

    @Autowired
    public TmdbDiscoverRequestFactory(
            TmdbDiscoverFilterRegistry registry,
            TmdbNameResolver nameResolver
    ) {
        this.registry = registry;
        this.nameResolver = nameResolver;
    }

    public TmdbDiscoverRequest create(SearchIntent intent, AiMediaType mediaType) {
        List<RenderedCriterion> rendered = new ArrayList<>();
        List<NamedFilterCriterion> rankingOnly = new ArrayList<>();
        String watchRegion = intent.textFilters().stream()
                .filter(value -> value.key() == TextFilterKey.WATCH_REGION)
                .map(TextFilterCriterion::value)
                .findFirst()
                .orElse(null);
        boolean hasCertificationTerritory = intent.textFilters().stream()
                .anyMatch(value -> value.key() == TextFilterKey.CERTIFICATION_COUNTRY
                        || value.key() == TextFilterKey.REGION);

        for (NumericFilterCriterion criterion : intent.numericFilters()) {
            add(rendered, criterion.key(), mediaType, criterion.operator(),
                    FilterPolarity.INCLUDE, formatNumber(criterion.value()));
        }
        for (DateFilterCriterion criterion : intent.dateFilters()) {
            add(rendered, criterion.key(), mediaType, criterion.operator(),
                    FilterPolarity.INCLUDE, criterion.value());
        }
        for (TextFilterCriterion criterion : intent.textFilters()) {
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            if (dependencySatisfied(definition, hasCertificationTerritory, watchRegion)) {
                add(rendered, criterion.key(), mediaType, FilterOperator.EQ,
                        FilterPolarity.INCLUDE, criterion.value());
            }
        }
        for (BooleanFilterCriterion criterion : intent.booleanFilters()) {
            add(rendered, criterion.key(), mediaType, FilterOperator.EQ,
                    FilterPolarity.INCLUDE, criterion.value().toString());
        }
        for (EnumFilterCriterion criterion : intent.enumFilters()) {
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            if (definition == null || !definition.supports(mediaType)) {
                continue;
            }
            if (!dependencySatisfied(definition, hasCertificationTerritory, watchRegion)) {
                continue;
            }
            List<String> values = criterion.values().stream()
                    .filter(value -> enumValueSupported(criterion.key(), value, mediaType))
                    .map(definition.renderedValues()::get)
                    .toList();
            if (!values.isEmpty()) {
                add(rendered, criterion.key(), mediaType, FilterOperator.EQ,
                        FilterPolarity.INCLUDE, join(values, criterion.matching()));
            }
        }
        for (NamedFilterCriterion criterion : intent.namedFilters()) {
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            if (definition == null || !definition.supports(mediaType)) {
                rankingOnly.add(criterion);
                continue;
            }
            if (!dependencySatisfied(definition, hasCertificationTerritory, watchRegion)) {
                rankingOnly.add(criterion);
                continue;
            }
            List<String> ids = new ArrayList<>();
            List<String> unresolved = new ArrayList<>();
            for (String name : criterion.names()) {
                OptionalInt id = nameResolver.resolve(
                        criterion.key(), name, criterion.evidence(), mediaType, watchRegion
                );
                if (id.isPresent()) {
                    ids.add(String.valueOf(id.getAsInt()));
                } else {
                    unresolved.add(name);
                }
            }
            if (!ids.isEmpty()) {
                add(rendered, criterion.key(), mediaType, FilterOperator.EQ,
                        criterion.polarity(), join(ids, criterion.matching()));
            }
            if (!unresolved.isEmpty()) {
                rankingOnly.add(new NamedFilterCriterion(
                        criterion.key(), unresolved, criterion.polarity(),
                        criterion.matching(), criterion.evidence()
                ));
            }
        }
        return new TmdbDiscoverRequest(mediaType, rendered, rankingOnly);
    }

    private boolean dependencySatisfied(
            TmdbDiscoverFilterDefinition definition,
            boolean hasCertificationTerritory,
            String watchRegion
    ) {
        return switch (definition.dependency()) {
            case NONE -> true;
            case CERTIFICATION_TERRITORY -> hasCertificationTerritory;
            case WATCH_REGION -> watchRegion != null;
        };
    }

    private void add(
            List<RenderedCriterion> target,
            Enum<?> key,
            AiMediaType mediaType,
            FilterOperator operator,
            FilterPolarity polarity,
            String value
    ) {
        TmdbDiscoverFilterDefinition definition = registry.definition(key);
        if (definition == null || !definition.supports(mediaType)) {
            return;
        }
        String parameter = definition.parameter(mediaType, operator, polarity);
        if (parameter != null) {
            target.add(new RenderedCriterion(parameter, value));
        }
    }

    private boolean enumValueSupported(
            EnumFilterKey key,
            String value,
            AiMediaType mediaType
    ) {
        if (key != EnumFilterKey.SORT_BY) {
            return true;
        }
        boolean movieOnly = value.startsWith("PRIMARY_RELEASE_DATE")
                || value.startsWith("TITLE_") || value.startsWith("ORIGINAL_TITLE_")
                || value.startsWith("REVENUE_");
        boolean seriesOnly = value.startsWith("FIRST_AIR_DATE")
                || value.startsWith("NAME_") || value.startsWith("ORIGINAL_NAME_");
        return (!movieOnly || mediaType == AiMediaType.MOVIE)
                && (!seriesOnly || mediaType == AiMediaType.SERIES);
    }

    private String join(List<String> values, FilterMatch matching) {
        return values.stream().distinct().collect(Collectors.joining(
                matching == FilterMatch.ALL ? "," : "|"
        ));
    }

    private String formatNumber(Double value) {
        if (value % 1 == 0) {
            return Long.toString(value.longValue());
        }
        return String.format(Locale.ROOT, "%s", value);
    }
}
