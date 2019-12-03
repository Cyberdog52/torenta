package ch.andreskonrad.torenta.bittorrent.controller;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.service.BitTorrentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/bittorrent")
public class BittorrentController {

    private final BitTorrentService bitTorrentService;

    @Autowired
    public BittorrentController(BitTorrentService bitTorrentService) {
        this.bitTorrentService = bitTorrentService;
    }

    @PostMapping()
    public ResponseEntity<HttpStatus> startDownload(@RequestBody String magnetLink) {
        try {
            this.bitTorrentService.startDownloadToDownloadsFolder(magnetLink);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<Set<Integer>> getTorrentIds() {
        Set<Integer> result;
        try {
            result = this.bitTorrentService.getTorrentIds();
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DownloadDto> getDownloadDto(@PathVariable("id") int id) {
        DownloadDto result;
        try {
            result = BitTorrentService.getDownload(id).mapToDownloadDto();
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
