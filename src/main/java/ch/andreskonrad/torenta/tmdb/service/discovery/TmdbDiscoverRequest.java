package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import ch.andreskonrad.torenta.concierge.dto.NamedFilterCriterion;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TmdbDiscoverRequest {

    private static final Set<String> SINGLE_VALUE_PARAMETERS = Set.of(
            "page", "sort_by", "include_adult", "include_video",
            "include_null_first_air_dates", "screened_theatrically", "language"
    );

    private final AiMediaType mediaType;
    private final List<RenderedCriterion> criteria;
    private final List<NamedFilterCriterion> rankingOnlyCriteria;

    TmdbDiscoverRequest(
            AiMediaType mediaType,
            List<RenderedCriterion> criteria,
            List<NamedFilterCriterion> rankingOnlyCriteria
    ) {
        if (mediaType != AiMediaType.MOVIE && mediaType != AiMediaType.SERIES) {
            throw new IllegalArgumentException("A TMDB discover request requires one media type");
        }
        this.mediaType = mediaType;
        this.criteria = List.copyOf(criteria);
        this.rankingOnlyCriteria = List.copyOf(rankingOnlyCriteria);
    }

    public AiMediaType mediaType() {
        return mediaType;
    }

    public List<NamedFilterCriterion> rankingOnlyCriteria() {
        return rankingOnlyCriteria;
    }

    public void applyTo(UriComponentsBuilder builder) {
        builder.replaceQueryParam("page", "1")
                .replaceQueryParam("sort_by", "popularity.desc")
                .replaceQueryParam("include_adult", false);
        if (mediaType == AiMediaType.MOVIE) {
            builder.replaceQueryParam("include_video", false);
        } else {
            builder.replaceQueryParam("include_null_first_air_dates", false);
        }
        for (RenderedCriterion criterion : criteria) {
            if (SINGLE_VALUE_PARAMETERS.contains(criterion.parameter())) {
                builder.replaceQueryParam(criterion.parameter(), criterion.value());
            } else {
                builder.queryParam(criterion.parameter(), criterion.value());
            }
        }
    }

    record RenderedCriterion(String parameter, String value) {
    }

    @Override
    public String toString() {
        return "TmdbDiscoverRequest[mediaType=" + mediaType + ", criteria=" + criteria
                + ", rankingOnlyCriteria=" + rankingOnlyCriteria + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TmdbDiscoverRequest request)) {
            return false;
        }
        return mediaType == request.mediaType
                && criteria.equals(request.criteria)
                && rankingOnlyCriteria.equals(request.rankingOnlyCriteria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaType, criteria, rankingOnlyCriteria);
    }
}
