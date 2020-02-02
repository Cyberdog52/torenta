package ch.andreskonrad.torenta.library.controller;

import ch.andreskonrad.torenta.library.dto.TvLibrary;
import ch.andreskonrad.torenta.library.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/")
public class LibraryController {


    private final LibraryService libraryService;

    @Autowired
    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/tv")
    public ResponseEntity<TvLibrary> getTvLibrary() {
        TvLibrary tvLibrary;
        try {
            tvLibrary = this.libraryService.getTvLibrary();
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(tvLibrary, HttpStatus.OK);
    }




}
