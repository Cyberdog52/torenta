package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TmdbService.class)
@EnableConfigurationProperties
public class TmdbServiceTest {

    @Autowired
    private TmdbService tmdbService;

    @Disabled
    @Test
    public void manualSearch() {
        TmdbSeriesSearchResultDto result = tmdbService.searchSeries("Walking Dead");

        assertTrue(result.getResults().size() > 0);
    }

    @Disabled
    @Test
    public void manualGetSeries() {
        TmdbSeriesDetailDto result = tmdbService.getSeries(1402);

        assertEquals(1402, result.getId());
    }
}
