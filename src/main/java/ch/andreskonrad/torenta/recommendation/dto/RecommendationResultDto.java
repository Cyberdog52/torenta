package ch.andreskonrad.torenta.recommendation.dto;

import java.util.List;

public class RecommendationResultDto {

    private final int seriesConsidered;
    private final List<String> unresolvedSeriesNames;
    private final List<SeriesRecommendationDto> recommendations;

    public RecommendationResultDto(
            int seriesConsidered,
            List<String> unresolvedSeriesNames,
            List<SeriesRecommendationDto> recommendations
    ) {
        this.seriesConsidered = seriesConsidered;
        this.unresolvedSeriesNames = unresolvedSeriesNames;
        this.recommendations = recommendations;
    }

    /** Total number of series folders that were scanned (after any recency filtering). */
    public int getSeriesConsidered() {
        return seriesConsidered;
    }

    /**
     * Series folders that could not be matched to a TMDB entry (or whose TMDB lookup failed),
     * surfaced so a user can tell "nothing missing" apart from "couldn't check this series"
     * without needing to read server logs.
     */
    public List<String> getUnresolvedSeriesNames() {
        return unresolvedSeriesNames;
    }

    public List<SeriesRecommendationDto> getRecommendations() {
        return recommendations;
    }
}
