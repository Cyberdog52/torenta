package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterDefinition;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverFilterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class SearchIntentNormalizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIntentNormalizer.class);
    private static final int MAX_CRITERIA_PER_CATEGORY = 50;
    private static final int MAX_VALUES_PER_CRITERION = 10;
    private static final int MAX_TEXT_LENGTH = 200;

    private final TmdbDiscoverFilterRegistry registry;

    @Autowired
    public SearchIntentNormalizer(TmdbDiscoverFilterRegistry registry) {
        this.registry = registry;
    }

    public SearchIntent normalize(SearchIntent intent, String prompt) {
        if (intent == null) {
            throw new IllegalStateException("AI provider returned no search intent");
        }
        String normalizedPrompt = normalizeWhitespace(prompt).toLowerCase(Locale.ROOT);
        AiMediaType mediaType = intent.mediaType() == null ? AiMediaType.ANY : intent.mediaType();
        int supplied = suppliedCount(intent);

        List<NumericFilterCriterion> numeric = normalizeNumeric(
                bounded(intent.numericFilters()), normalizedPrompt, mediaType
        );
        List<DateFilterCriterion> dates = normalizeDates(
                bounded(intent.dateFilters()), normalizedPrompt, mediaType
        );
        List<TextFilterCriterion> text = normalizeText(
                bounded(intent.textFilters()), normalizedPrompt, mediaType
        );
        List<BooleanFilterCriterion> booleans = normalizeBooleans(
                bounded(intent.booleanFilters()), normalizedPrompt, mediaType
        );
        List<NamedFilterCriterion> named = normalizeNamed(
                bounded(intent.namedFilters()), normalizedPrompt, mediaType
        );
        List<EnumFilterCriterion> enums = normalizeEnums(
                bounded(intent.enumFilters()), normalizedPrompt, mediaType
        );

        numeric = removeContradictoryNumericBounds(numeric);
        dates = removeContradictoryDateBounds(dates);

        SearchIntent normalized = new SearchIntent(
                mediaType,
                normalizeWords(intent.moods()),
                normalizeSimilarTo(intent.similarTo(), normalizedPrompt),
                numeric,
                dates,
                text,
                booleans,
                named,
                enums
        );
        int accepted = suppliedCount(normalized);
        if (accepted < supplied) {
            LOGGER.debug("Omitted {} unsupported or ungrounded concierge criteria", supplied - accepted);
        }
        return normalized;
    }

    private List<NumericFilterCriterion> normalizeNumeric(
            List<NumericFilterCriterion> criteria,
            String prompt,
            AiMediaType mediaType
    ) {
        List<NumericFilterCriterion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (NumericFilterCriterion criterion : criteria) {
            if (criterion == null || criterion.key() == null || criterion.operator() == null
                    || criterion.value() == null || !Double.isFinite(criterion.value())
                    || !evidenced(criterion.evidence(), prompt)) {
                continue;
            }
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            if (!supported(definition, mediaType)
                    || !definition.operators().contains(criterion.operator())
                    || criterion.value() < definition.minimum()
                    || criterion.value() > definition.maximum()) {
                continue;
            }
            if ((criterion.key() == NumericFilterKey.YEAR
                    || criterion.key() == NumericFilterKey.PRIMARY_RELEASE_YEAR
                    || criterion.key() == NumericFilterKey.FIRST_AIR_DATE_YEAR
                    || criterion.key() == NumericFilterKey.VOTE_COUNT
                    || criterion.key() == NumericFilterKey.RUNTIME
                    || criterion.key() == NumericFilterKey.PAGE)
                    && criterion.value() % 1 != 0) {
                continue;
            }
            String identity = criterion.key() + ":" + criterion.operator();
            if (seen.add(identity)) {
                result.add(new NumericFilterCriterion(
                        criterion.key(),
                        criterion.operator(),
                        criterion.value(),
                        normalizeWhitespace(criterion.evidence())
                ));
            }
        }
        return List.copyOf(result);
    }

    private List<DateFilterCriterion> normalizeDates(
            List<DateFilterCriterion> criteria,
            String prompt,
            AiMediaType mediaType
    ) {
        List<DateFilterCriterion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DateFilterCriterion criterion : criteria) {
            if (criterion == null || criterion.key() == null || criterion.operator() == null
                    || criterion.value() == null || !evidenced(criterion.evidence(), prompt)) {
                continue;
            }
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            if (!supported(definition, mediaType)
                    || !definition.operators().contains(criterion.operator())) {
                continue;
            }
            try {
                LocalDate.parse(criterion.value());
            } catch (DateTimeParseException exception) {
                continue;
            }
            String identity = criterion.key() + ":" + criterion.operator();
            if (seen.add(identity)) {
                result.add(new DateFilterCriterion(
                        criterion.key(),
                        criterion.operator(),
                        criterion.value(),
                        normalizeWhitespace(criterion.evidence())
                ));
            }
        }
        return List.copyOf(result);
    }

    private List<TextFilterCriterion> normalizeText(
            List<TextFilterCriterion> criteria,
            String prompt,
            AiMediaType mediaType
    ) {
        List<TextFilterCriterion> result = new ArrayList<>();
        Set<TextFilterKey> seen = new HashSet<>();
        for (TextFilterCriterion criterion : criteria) {
            if (criterion == null || criterion.key() == null || criterion.value() == null
                    || !evidenced(criterion.evidence(), prompt)) {
                continue;
            }
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            String value = normalizeTextValue(criterion.key(), criterion.value());
            if (!supported(definition, mediaType) || value == null
                    || !value.matches(definition.validationPattern())
                    || !validTimezone(criterion.key(), value)) {
                continue;
            }
            if (seen.add(criterion.key())) {
                result.add(new TextFilterCriterion(
                        criterion.key(), value, normalizeWhitespace(criterion.evidence())
                ));
            }
        }
        return List.copyOf(result);
    }

    private List<BooleanFilterCriterion> normalizeBooleans(
            List<BooleanFilterCriterion> criteria,
            String prompt,
            AiMediaType mediaType
    ) {
        List<BooleanFilterCriterion> result = new ArrayList<>();
        Set<BooleanFilterKey> seen = new HashSet<>();
        for (BooleanFilterCriterion criterion : criteria) {
            if (criterion == null || criterion.key() == null || criterion.value() == null
                    || !evidenced(criterion.evidence(), prompt)
                    || !supported(registry.definition(criterion.key()), mediaType)) {
                continue;
            }
            if (seen.add(criterion.key())) {
                result.add(new BooleanFilterCriterion(
                        criterion.key(), criterion.value(), normalizeWhitespace(criterion.evidence())
                ));
            }
        }
        return List.copyOf(result);
    }

    private List<NamedFilterCriterion> normalizeNamed(
            List<NamedFilterCriterion> criteria,
            String prompt,
            AiMediaType mediaType
    ) {
        List<NamedFilterCriterion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (NamedFilterCriterion criterion : criteria) {
            FilterPolarity polarity = criterion == null || criterion.polarity() == null
                    ? FilterPolarity.INCLUDE : criterion.polarity();
            FilterMatch matching = criterion == null || criterion.matching() == null
                    ? FilterMatch.ANY : criterion.matching();
            if (criterion == null || criterion.key() == null
                    || !evidenced(criterion.evidence(), prompt)) {
                continue;
            }
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            List<String> names = normalizeNames(criterion.names());
            if (definition == null || names.isEmpty()
                    || !definition.polarities().contains(polarity)) {
                continue;
            }
            String identity = criterion.key() + ":" + polarity;
            if (seen.add(identity)) {
                result.add(new NamedFilterCriterion(
                        criterion.key(), names, polarity, matching,
                        normalizeWhitespace(criterion.evidence())
                ));
            }
        }
        return List.copyOf(result);
    }

    private List<EnumFilterCriterion> normalizeEnums(
            List<EnumFilterCriterion> criteria,
            String prompt,
            AiMediaType mediaType
    ) {
        List<EnumFilterCriterion> result = new ArrayList<>();
        Set<EnumFilterKey> seen = new HashSet<>();
        for (EnumFilterCriterion criterion : criteria) {
            if (criterion == null || criterion.key() == null
                    || !evidenced(criterion.evidence(), prompt)) {
                continue;
            }
            TmdbDiscoverFilterDefinition definition = registry.definition(criterion.key());
            if (!supported(definition, mediaType)) {
                continue;
            }
            List<String> values = normalizeWords(criterion.values()).stream()
                    .filter(definition.renderedValues()::containsKey)
                    .limit(maxValuesForEnum(criterion.key()))
                    .toList();
            if (!values.isEmpty() && seen.add(criterion.key())) {
                result.add(new EnumFilterCriterion(
                        criterion.key(),
                        values,
                        criterion.matching() == null ? FilterMatch.ANY : criterion.matching(),
                        normalizeWhitespace(criterion.evidence())
                ));
            }
        }
        return List.copyOf(result);
    }

    private List<NumericFilterCriterion> removeContradictoryNumericBounds(
            List<NumericFilterCriterion> criteria
    ) {
        Set<NumericFilterKey> contradictory = new HashSet<>();
        for (NumericFilterKey key : NumericFilterKey.values()) {
            Double lower = numericValue(criteria, key, FilterOperator.GTE);
            Double upper = numericValue(criteria, key, FilterOperator.LTE);
            if (lower != null && upper != null && lower > upper) {
                contradictory.add(key);
            }
        }
        return criteria.stream().filter(value -> !contradictory.contains(value.key())).toList();
    }

    private List<DateFilterCriterion> removeContradictoryDateBounds(
            List<DateFilterCriterion> criteria
    ) {
        Set<DateFilterKey> contradictory = new HashSet<>();
        for (DateFilterKey key : DateFilterKey.values()) {
            LocalDate lower = dateValue(criteria, key, FilterOperator.GTE);
            LocalDate upper = dateValue(criteria, key, FilterOperator.LTE);
            if (lower != null && upper != null && lower.isAfter(upper)) {
                contradictory.add(key);
            }
        }
        return criteria.stream().filter(value -> !contradictory.contains(value.key())).toList();
    }

    private Double numericValue(
            List<NumericFilterCriterion> criteria,
            NumericFilterKey key,
            FilterOperator operator
    ) {
        return criteria.stream()
                .filter(value -> value.key() == key && value.operator() == operator)
                .map(NumericFilterCriterion::value)
                .findFirst()
                .orElse(null);
    }

    private LocalDate dateValue(
            List<DateFilterCriterion> criteria,
            DateFilterKey key,
            FilterOperator operator
    ) {
        return criteria.stream()
                .filter(value -> value.key() == key && value.operator() == operator)
                .map(DateFilterCriterion::value)
                .map(LocalDate::parse)
                .findFirst()
                .orElse(null);
    }

    private boolean supported(
            TmdbDiscoverFilterDefinition definition,
            AiMediaType mediaType
    ) {
        if (definition == null) {
            return false;
        }
        return mediaType == AiMediaType.ANY
                ? definition.supports(AiMediaType.MOVIE) || definition.supports(AiMediaType.SERIES)
                : definition.supports(mediaType);
    }

    private String normalizeTextValue(TextFilterKey key, String raw) {
        String value = normalizeWhitespace(raw);
        if (value.isEmpty() || value.length() > MAX_TEXT_LENGTH) {
            return null;
        }
        return switch (key) {
            case LANGUAGE -> {
                String[] parts = value.replace('_', '-').split("-", 2);
                yield parts[0].toLowerCase(Locale.ROOT)
                        + (parts.length == 2 ? "-" + parts[1].toUpperCase(Locale.ROOT) : "");
            }
            case ORIGINAL_LANGUAGE -> value.toLowerCase(Locale.ROOT);
            case ORIGIN_COUNTRY, REGION, WATCH_REGION, CERTIFICATION_COUNTRY ->
                    value.toUpperCase(Locale.ROOT);
            case CERTIFICATION, CERTIFICATION_GTE, CERTIFICATION_LTE ->
                    value.toUpperCase(Locale.ROOT);
            case TIMEZONE -> value;
        };
    }

    private boolean validTimezone(TextFilterKey key, String value) {
        if (key != TextFilterKey.TIMEZONE) {
            return true;
        }
        try {
            ZoneId.of(value);
            return true;
        } catch (DateTimeException exception) {
            return false;
        }
    }

    private boolean evidenced(String evidence, String prompt) {
        String normalized = normalizeWhitespace(evidence).toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && normalized.length() <= MAX_TEXT_LENGTH
                && prompt.contains(normalized);
    }

    private String normalizeSimilarTo(String value, String prompt) {
        String normalized = nullable(value);
        if (normalized == null
                || !(prompt.matches(".*\\b(similar\\s+to|like|resembling)\\b.*"))) {
            return null;
        }
        return normalized;
    }

    private List<String> normalizeNames(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        values.stream()
                .filter(Objects::nonNull)
                .map(SearchIntentNormalizer::normalizeWhitespace)
                .filter(value -> !value.isEmpty() && value.length() <= MAX_TEXT_LENGTH)
                .limit(MAX_VALUES_PER_CRITERION)
                .forEach(normalized::add);
        return List.copyOf(normalized);
    }

    private List<String> normalizeWords(List<String> values) {
        return normalizeNames(values).stream()
                .map(value -> value.toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_'))
                .distinct()
                .toList();
    }

    private int maxValuesForEnum(EnumFilterKey key) {
        return key == EnumFilterKey.SORT_BY ? 1 : MAX_VALUES_PER_CRITERION;
    }

    private <T> List<T> bounded(List<T> values) {
        return values == null ? List.of() : values.stream()
                .limit(MAX_CRITERIA_PER_CATEGORY)
                .toList();
    }

    private int suppliedCount(SearchIntent intent) {
        return size(intent.numericFilters()) + size(intent.dateFilters()) + size(intent.textFilters())
                + size(intent.booleanFilters()) + size(intent.namedFilters()) + size(intent.enumFilters());
    }

    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private String nullable(String value) {
        String normalized = normalizeWhitespace(value);
        return normalized.isEmpty() || normalized.length() > MAX_TEXT_LENGTH ? null : normalized;
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
