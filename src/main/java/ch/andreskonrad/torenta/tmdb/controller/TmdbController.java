package ch.andreskonrad.torenta.tmdb.controller;

import ch.andreskonrad.torenta.tmdb.dto.SearchResult;
import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("tv/detail")
    public ResponseEntity<SeriesDetail> getTVShow(@RequestParam("id") int id) {
        SeriesDetail detail;
        try {
            detail = this.tmdbService.get(id);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(detail, HttpStatus.OK);
    }


}
