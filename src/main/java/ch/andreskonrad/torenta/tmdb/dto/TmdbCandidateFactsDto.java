package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCandidateFactsDto {
    private int runtime;
    private List<Integer> episode_run_time;
    private List<TmdbNamedEntityDto> genres;
    private List<TmdbNamedEntityDto> production_companies;
    private List<TmdbNamedEntityDto> networks;
    private Credits credits;
    private Keywords keywords;
    private ReleaseDates release_dates;
    private ContentRatings content_ratings;

    @JsonProperty("watch/providers")
    private WatchProviders watchProviders;

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Credits {
        private List<TmdbNamedEntityDto> cast;
        private List<TmdbNamedEntityDto> crew;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Keywords {
        private List<TmdbNamedEntityDto> results;
        private List<TmdbNamedEntityDto> keywords;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReleaseDates {
        private List<ReleaseDateCountry> results;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReleaseDateCountry {
        private List<ReleaseDate> release_dates;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReleaseDate {
        private String certification;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentRatings {
        private List<ContentRating> results;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentRating {
        private String rating;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WatchProviders {
        private Map<String, WatchProviderRegion> results;
    }

    @NoArgsConstructor
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WatchProviderRegion {
        private List<TmdbWatchProviderDto> flatrate;
        private List<TmdbWatchProviderDto> free;
        private List<TmdbWatchProviderDto> ads;
        private List<TmdbWatchProviderDto> rent;
        private List<TmdbWatchProviderDto> buy;
    }
}
