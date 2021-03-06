package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbNetworkDto {

    private String name;
    private int id;
    private String logo_path;
    private String origin_country;

    public TmdbNetworkDto() {
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getLogo_path() {
        return logo_path;
    }

    public String getOrigin_country() {
        return origin_country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TmdbNetworkDto network = (TmdbNetworkDto) o;
        return id == network.id &&
                Objects.equals(name, network.name) &&
                Objects.equals(logo_path, network.logo_path) &&
                Objects.equals(origin_country, network.origin_country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, logo_path, origin_country);
    }
}
