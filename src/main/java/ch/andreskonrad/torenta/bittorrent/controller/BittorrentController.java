package ch.andreskonrad.torenta.bittorrent.controller;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.service.BitTorrentService;
import ch.andreskonrad.torenta.bittorrent.service.DownloadNotFoundException;
import ch.andreskonrad.torenta.bittorrent.service.DownloadOperationException;
import ch.andreskonrad.torenta.bittorrent.service.InvalidDownloadStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/bittorrent")
@Slf4j
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
            log.error(exception.getMessage(), exception);
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

    @PostMapping("/{id}/pause")
    public ResponseEntity<HttpStatus> pause(@PathVariable String id) {
        return runLifecycleAction(() -> bitTorrentService.pause(id));
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<HttpStatus> restart(@PathVariable String id) {
        return runLifecycleAction(() -> bitTorrentService.restart(id));
    }

    /** Stops the engine (if running) and permanently deletes this download's owned files. */
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> stopAndDelete(@PathVariable String id) {
        return runLifecycleAction(() -> bitTorrentService.stopAndDelete(id));
    }

    /** Removes a finished tile's metadata only; the final media files are preserved. */
    @DeleteMapping("/{id}/tile")
    public ResponseEntity<HttpStatus> remove(@PathVariable String id) {
        return runLifecycleAction(() -> bitTorrentService.remove(id));
    }

    private ResponseEntity<HttpStatus> runLifecycleAction(Runnable action) {
        try {
            action.run();
        } catch (DownloadNotFoundException exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (InvalidDownloadStateException exception) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (DownloadOperationException exception) {
            log.error(exception.getMessage(), exception);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
