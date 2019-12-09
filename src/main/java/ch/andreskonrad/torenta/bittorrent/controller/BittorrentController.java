package ch.andreskonrad.torenta.bittorrent.controller;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
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
    public ResponseEntity<HttpStatus> startDownload(@RequestBody DownloadRequest downloadRequest) {
        try {
            this.bitTorrentService.startDownloadToPreferredFolder(downloadRequest);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<Set<DownloadDto>> getTorrents() {
        Set<DownloadDto> result;
        try {
            result = this.bitTorrentService.getAllDownloadDtos();
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
