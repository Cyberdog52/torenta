package ch.andreskonrad.torenta.concierge.dto;

import java.util.List;

public record NamedFilterCriterion(
        NamedFilterKey key,
        List<String> names,
        FilterPolarity polarity,
        FilterMatch matching,
        String evidence
) {
}
