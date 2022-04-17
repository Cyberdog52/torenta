package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TmdbService.class)
@EnableConfigurationProperties
public class TmdbServiceTest {

    @Autowired
    private TmdbService tmdbService;

    @Ignore
    @Test
    public void manualSearch() {
        TmdbSeriesSearchResultDto result = tmdbService.searchSeries("Walking Dead");

        assertTrue(result.getResults().size() > 0);
    }

    @Ignore
    @Test
    public void manualGetSeries() {
        TmdbSeriesDetailDto result = tmdbService.getSeries(1402);

        assertEquals(1402, result.getId());
    }
}
