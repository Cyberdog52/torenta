package ch.andreskonrad.torenta.tmdb.dto;

import java.util.List;
import java.util.Objects;

public class SeriesDetail {

    private String backdrop_path;
    private List<Creator> created_by;
    private List<Integer> episode_run_time;
    private String first_air_date;
    private List<Genre> genres;
    private String homepage;
    private int id;
    private boolean in_production;
    private List<String> languages;
    private String last_air_date;
    private Episode last_episode_to_air;
    private String name;
    private Episode next_episode_to_air;
    private List<Network> networks;
    private int number_of_episodes;
    private int number_of_seasons;
    private List<String> origin_country;
    private String original_language;
    private String original_name;
    private String overview;
    private double popularity;
    private String poster_path;
    private List<ProductionCompany> production_companies;
    private List<Season> seasons;
    private String status;
    private String type;
    private double vote_average;
    private int vote_count;

    //used for jackson
    public SeriesDetail() {
    }

    public String getBackdrop_path() {
        return backdrop_path;
    }

    public List<Creator> getCreated_by() {
        return created_by;
    }

    public List<Integer> getEpisode_run_time() {
        return episode_run_time;
    }

    public String getFirst_air_date() {
        return first_air_date;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public String getHomepage() {
        return homepage;
    }

    public int getId() {
        return id;
    }

    public boolean isIn_production() {
        return in_production;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public String getLast_air_date() {
        return last_air_date;
    }

    public Episode getLast_episode_to_air() {
        return last_episode_to_air;
    }

    public String getName() {
        return name;
    }

    public Episode getNext_episode_to_air() {
        return next_episode_to_air;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public int getNumber_of_episodes() {
        return number_of_episodes;
    }

    public int getNumber_of_seasons() {
        return number_of_seasons;
    }

    public List<String> getOrigin_country() {
        return origin_country;
    }

    public String getOriginal_language() {
        return original_language;
    }

    public String getOriginal_name() {
        return original_name;
    }

    public String getOverview() {
        return overview;
    }

    public double getPopularity() {
        return popularity;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public List<ProductionCompany> getProduction_companies() {
        return production_companies;
    }

    public List<Season> getSeasons() {
        return seasons;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public double getVote_average() {
        return vote_average;
    }

    public int getVote_count() {
        return vote_count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeriesDetail detail = (SeriesDetail) o;
        return id == detail.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
