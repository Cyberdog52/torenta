package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Getter
public class TmdbSeriesOverviewDto {

    private String original_name;
    private List<Integer> genre_ids;
    private String name;
    private double popularity;
    private List<String> origin_country;
    private int vote_count;
    private String first_air_date;
    private String backdrop_path;
    private String original_language;
    private int id;
    private double vote_average;
    private String overview;
    private String poster_path;
}
