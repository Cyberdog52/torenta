package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Getter
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbSeriesDetailDto {

    private String backdrop_path;
    private List<TmdbCreatorDto> created_by;
    private List<Integer> episode_run_time;
    private String first_air_date;
    private List<TmdbGenreDto> genres;
    private String homepage;
    private int id;
    private boolean in_production;
    private List<String> languages;
    private String last_air_date;
    private TmdbEpisodeDto last_episode_to_air;
    private String name;
    private TmdbEpisodeDto next_episode_to_air;
    private List<TmdbNetworkDto> networks;
    private int number_of_episodes;
    private int number_of_seasons;
    private List<String> origin_country;
    private String original_language;
    private String original_name;
    private String overview;
    private double popularity;
    private String poster_path;
    private List<TmdbProductionCompanyDto> production_companies;
    private List<TmdbSeasonDto> seasons;
    private String status;
    private String type;
    private double vote_average;
    private int vote_count;
    private String tagline;
}
