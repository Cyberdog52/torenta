package ch.andreskonrad.torenta.concierge.dto;

import java.util.List;

public record EnumFilterCriterion(
        EnumFilterKey key,
        List<String> values,
        FilterMatch matching,
        String evidence
) {
}
