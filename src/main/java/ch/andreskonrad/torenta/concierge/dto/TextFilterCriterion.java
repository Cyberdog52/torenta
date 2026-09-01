package ch.andreskonrad.torenta.concierge.dto;

public record TextFilterCriterion(
        TextFilterKey key,
        String value,
        String evidence
) {
}
