package ch.andreskonrad.torenta.torrent.controller;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.service.TorrentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/torrent")
public class TorrentController {

    private final TorrentService torrentService;

    @Autowired
    public TorrentController(TorrentService torrentService) {
        this.torrentService = torrentService;
    }

    @GetMapping()
    public ResponseEntity<List<TorrentEntry>> search(@RequestParam("search") String searchString) {
        List<TorrentEntry> torrentEntries;
        try {
            torrentEntries = this.torrentService.search(searchString);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(torrentEntries, HttpStatus.OK);
    }
}