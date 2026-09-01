package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import ch.andreskonrad.torenta.concierge.dto.FilterOperator;
import ch.andreskonrad.torenta.concierge.dto.FilterPolarity;

import java.util.Map;
import java.util.Set;

public record TmdbDiscoverFilterDefinition(
        Enum<?> key,
        ValueKind valueKind,
        Set<AiMediaType> mediaTypes,
        Set<FilterOperator> operators,
        Map<AiMediaType, Map<FilterOperator, String>> parameters,
        Map<AiMediaType, String> excludeParameters,
        Set<FilterPolarity> polarities,
        Double minimum,
        Double maximum,
        String validationPattern,
        Map<String, String> renderedValues,
        Dependency dependency
) {
    public enum ValueKind {
        NUMERIC,
        DATE,
        TEXT,
        BOOLEAN,
        NAMED,
        ENUM
    }

    public enum Dependency {
        NONE,
        CERTIFICATION_TERRITORY,
        WATCH_REGION
    }

    public TmdbDiscoverFilterDefinition {
        mediaTypes = Set.copyOf(mediaTypes);
        operators = Set.copyOf(operators);
        parameters = Map.copyOf(parameters);
        excludeParameters = Map.copyOf(excludeParameters);
        polarities = Set.copyOf(polarities);
        renderedValues = Map.copyOf(renderedValues);
    }

    public boolean supports(AiMediaType mediaType) {
        return mediaTypes.contains(mediaType);
    }

    public String parameter(
            AiMediaType mediaType,
            FilterOperator operator,
            FilterPolarity polarity
    ) {
        if (polarity == FilterPolarity.EXCLUDE) {
            return excludeParameters.get(mediaType);
        }
        Map<FilterOperator, String> byOperator = parameters.get(mediaType);
        return byOperator == null ? null : byOperator.get(operator);
    }
}
