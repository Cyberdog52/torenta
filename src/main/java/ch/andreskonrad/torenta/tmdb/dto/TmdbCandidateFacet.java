package ch.andreskonrad.torenta.tmdb.dto;

public enum TmdbCandidateFacet {
    CREDITS("credits"),
    KEYWORDS("keywords"),
    CERTIFICATIONS_MOVIE("release_dates"),
    CERTIFICATIONS_SERIES("content_ratings"),
    WATCH_PROVIDERS("watch/providers");

    private final String parameterValue;

    TmdbCandidateFacet(String parameterValue) {
        this.parameterValue = parameterValue;
    }

    public String parameterValue() {
        return parameterValue;
    }
}
