package ch.andreskonrad.torenta.tmdb.controller;

import ch.andreskonrad.torenta.tmdb.dto.*;
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
    public ResponseEntity<TmdbSeriesSearchResultDto> searchSeries(@RequestParam("search") String searchString) {
        TmdbSeriesSearchResultDto result;
        try {
            result = this.tmdbService.searchSeries(searchString);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("movie")
    public ResponseEntity<TmdbMoviesSearchResultDto> searchMovies(@RequestParam("search") String searchString) {
        TmdbMoviesSearchResultDto result;
        try {
            result = this.tmdbService.searchMovies(searchString);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("tv/{id}")
    public ResponseEntity<TmdbSeriesDetailDto> getTVShow(@PathVariable("id") int id) {
        TmdbSeriesDetailDto detail;
        try {
            detail = this.tmdbService.getSeries(id);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(detail, HttpStatus.OK);
    }

    @GetMapping("movie/{id}")
    public ResponseEntity<TmdbMovieDetailDto> getMovie(@PathVariable("id") int id) {
        TmdbMovieDetailDto detail;
        try {
            detail = this.tmdbService.getMovie(id);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(detail, HttpStatus.OK);
    }

    @GetMapping("tv/{id}/season/{season_number}")
    public ResponseEntity<TmdbEpisodeDto[]> getEpisodes(@PathVariable("id") int seriesId, @PathVariable("season_number") int season_number) {
        TmdbEpisodeDto[] tmdbEpisodeDtos;
        try {
            tmdbEpisodeDtos = this.tmdbService.getEpisodes(seriesId, season_number);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(tmdbEpisodeDtos, HttpStatus.OK);
    }


}
