package ch.andreskonrad.torenta.preference.controller;


import ch.andreskonrad.torenta.preference.dto.UserPreference;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preference")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @Autowired
    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @PostMapping()
    public ResponseEntity<HttpStatus> save(@RequestBody UserPreference preferences) {
        try {
            this.preferenceService.save(preferences);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<UserPreference> getTorrentIds() {
        UserPreference result;
        try {
            result = this.preferenceService.loadPreferences();
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
