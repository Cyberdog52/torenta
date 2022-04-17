package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Getter
@EqualsAndHashCode(of = "id")
public class TmdbProductionCompanyDto {

    private int id;
    private String logo_path;
    private String name;
    private String origin_country;
}
