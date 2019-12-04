package ch.andreskonrad.torenta.tmdb.controller;

import ch.andreskonrad.torenta.tmdb.dto.Episode;
import ch.andreskonrad.torenta.tmdb.dto.SearchResult;
import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;

@RestController
@RequestMapping("/api/tmdb/")
public class TmdbController {

    private final TmdbService tmdbService;

    @Autowired
    public TmdbController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("tv")
    public ResponseEntity<SearchResult> searchTV(@RequestParam("search") String searchString) {
        SearchResult result;
        try {
            result = this.tmdbService.search(searchString);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("tv/{id}")
    public ResponseEntity<SeriesDetail> getTVShow(@PathVariable("id") int id) {
        SeriesDetail detail;
        try {
            detail = this.tmdbService.get(id);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(detail, HttpStatus.OK);
    }

    @GetMapping("tv/{id}/season/{season_number}")
    public ResponseEntity<Episode[]> getEpisodes(@PathVariable("id") int seriesId, @PathVariable("season_number") int season_number) {
        Episode[] detail;
        try {
            detail = this.tmdbService.getEpisodes(seriesId, season_number);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(detail, HttpStatus.OK);
    }


}
