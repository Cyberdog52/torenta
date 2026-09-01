package ch.andreskonrad.torenta.concierge.dto;

import java.util.List;

public record SearchIntent(
        AiMediaType mediaType,
        List<String> moods,
        String similarTo,
        List<NumericFilterCriterion> numericFilters,
        List<DateFilterCriterion> dateFilters,
        List<TextFilterCriterion> textFilters,
        List<BooleanFilterCriterion> booleanFilters,
        List<NamedFilterCriterion> namedFilters,
        List<EnumFilterCriterion> enumFilters
) {
}
