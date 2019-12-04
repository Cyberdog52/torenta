package ch.andreskonrad.torenta.piratebay.controller;

import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntry;
import ch.andreskonrad.torenta.piratebay.service.PirateBayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/pirateBay")
public class PirateBayController {

    private final PirateBayService pirateBayService;

    @Autowired
    public PirateBayController(PirateBayService pirateBayService) {
        this.pirateBayService = pirateBayService;
    }

    @GetMapping()
    public ResponseEntity<List<PirateBayEntry>> search(@RequestParam("search") String searchString) {
        List<PirateBayEntry> pirateBayEntries;
        try {
            pirateBayEntries = this.pirateBayService.search(searchString);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(pirateBayEntries, HttpStatus.OK);
    }
}