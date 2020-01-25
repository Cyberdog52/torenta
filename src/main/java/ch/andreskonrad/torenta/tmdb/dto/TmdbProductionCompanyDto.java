package ch.andreskonrad.torenta.tmdb.dto;

import java.util.Objects;

public class TmdbProductionCompanyDto {

    private int id;
    private String logo_path;
    private String name;
    private String origin_country;

    public TmdbProductionCompanyDto() {
    }

    public int getId() {
        return id;
    }

    public String getLogo_path() {
        return logo_path;
    }

    public String getName() {
        return name;
    }

    public String getOrigin_country() {
        return origin_country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TmdbProductionCompanyDto that = (TmdbProductionCompanyDto) o;
        return id == that.id &&
                Objects.equals(logo_path, that.logo_path) &&
                Objects.equals(name, that.name) &&
                Objects.equals(origin_country, that.origin_country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, logo_path, name, origin_country);
    }
}
