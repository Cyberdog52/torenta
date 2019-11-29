package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;
import ch.andreskonrad.torenta.tmdb.dto.SearchResult;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TmdbServiceTest {

    //TODO make autowired
    private TmdbService tmdbService = new TmdbService();

    @Ignore
    @Test
    public void manualSearch() {
        SearchResult result = tmdbService.search("Walking Dead");

        assertEquals(1, result.getPage());

    }

    @Ignore
    @Test
    public void manualGetSeries() {
        SeriesDetail result = tmdbService.get(1402);

        assertEquals(1402, result.getId());
    }
}
