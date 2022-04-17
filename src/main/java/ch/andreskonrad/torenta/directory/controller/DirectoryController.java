package ch.andreskonrad.torenta.directory.controller;

import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import io.swagger.models.parameters.QueryParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    @Autowired
    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }


    @GetMapping("/series/{seriesTitle}")
    public ResponseEntity<DirectoryDto> getSeries(@PathVariable String seriesTitle) {
        DirectoryDto result;
        try {
            result = this.directoryService.getSeriesDirectory(seriesTitle);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (result == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/movie/{movieTitle}")
    public ResponseEntity<DirectoryDto> getMovie(@PathVariable String movieTitle, @RequestParam Integer releaseYear) {
        DirectoryDto result;
        try {
            result = this.directoryService.getMovieDirectory(movieTitle, releaseYear);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (result == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}


