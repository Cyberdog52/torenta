package ch.andreskonrad.torenta.concierge.dto;

import java.util.List;

public record AiConciergeResponse(SearchIntent intent, List<AiRankedResult> results) {
}
