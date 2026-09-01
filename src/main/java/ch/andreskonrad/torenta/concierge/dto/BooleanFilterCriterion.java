package ch.andreskonrad.torenta.concierge.dto;

public record BooleanFilterCriterion(
        BooleanFilterKey key,
        Boolean value,
        String evidence
) {
}
