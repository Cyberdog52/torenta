package ch.andreskonrad.torenta.concierge.dto;

public record DateFilterCriterion(
        DateFilterKey key,
        FilterOperator operator,
        String value,
        String evidence
) {
}
