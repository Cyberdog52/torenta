package ch.andreskonrad.torenta.concierge.dto;

public record NumericFilterCriterion(
        NumericFilterKey key,
        FilterOperator operator,
        Double value,
        String evidence
) {
}
