package ch.andreskonrad.torenta.tmdb.controller;

import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
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
    public ResponseEntity<TmdbSearchResultDto> searchTV(@RequestParam("search") String searchString) {
        TmdbSearchResultDto result;
        try {
            result = this.tmdbService.search(searchString);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("tv/{id}")
    public ResponseEntity<TmdbSeriesDetailDto> getTVShow(@PathVariable("id") int id) {
        TmdbSeriesDetailDto detail;
        try {
            detail = this.tmdbService.get(id);
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
