package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCombinedCreditDto {
    private int id;
    private String media_type;
    private String title;
    private String original_title;
    private String name;
    private String original_name;
    private String overview;
    private String poster_path;
    private String release_date;
    private String first_air_date;
    private String original_language;
    private List<Integer> genre_ids;
    private double popularity;
    private double vote_average;
    private int vote_count;
}
