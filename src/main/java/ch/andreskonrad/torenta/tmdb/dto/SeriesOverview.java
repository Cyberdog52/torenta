package ch.andreskonrad.torenta.tmdb.dto;

import java.util.List;
import java.util.Objects;

public class SeriesOverview {

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

    //used for jackson
    public SeriesOverview() {
    }

    public String getOriginal_name() {
        return original_name;
    }

    public List<Integer> getGenre_ids() {
        return genre_ids;
    }

    public String getName() {
        return name;
    }

    public double getPopularity() {
        return popularity;
    }

    public List<String> getOrigin_country() {
        return origin_country;
    }

    public int getVote_count() {
        return vote_count;
    }

    public String getFirst_air_date() {
        return first_air_date;
    }

    public String getBackdrop_path() {
        return backdrop_path;
    }

    public String getOriginal_language() {
        return original_language;
    }

    public int getId() {
        return id;
    }

    public double getVote_average() {
        return vote_average;
    }

    public String getOverview() {
        return overview;
    }

    public String getPoster_path() {
        return poster_path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeriesOverview that = (SeriesOverview) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
