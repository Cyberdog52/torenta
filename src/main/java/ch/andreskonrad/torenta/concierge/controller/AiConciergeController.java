package ch.andreskonrad.torenta.concierge.controller;

import ch.andreskonrad.torenta.concierge.dto.AiConciergeRequest;
import ch.andreskonrad.torenta.concierge.dto.AiConciergeResponse;
import ch.andreskonrad.torenta.concierge.service.AiConciergeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concierge/")
public class AiConciergeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiConciergeController.class);

    private final AiConciergeService conciergeService;

    @Autowired
    public AiConciergeController(AiConciergeService conciergeService) {
        this.conciergeService = conciergeService;
    }

    @PostMapping("search")
    @Operation(
            summary = "Search for AI-ranked movies and series",
            description = "Extracts search intent, retrieves candidates from TMDB, and ranks them. "
                    + "This endpoint never starts downloads."
    )
    @ApiResponse(responseCode = "200", description = "Ranked recommendations")
    @ApiResponse(responseCode = "400", description = "Invalid prompt")
    @ApiResponse(responseCode = "404", description = "AI provider or TMDB request failed")
    public ResponseEntity<AiConciergeResponse> search(@RequestBody AiConciergeRequest request) {
        try {
            AiConciergeResponse response = conciergeService.search(
                    request == null ? null : request.prompt()
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException exception) {
            LOGGER.debug("Rejected invalid concierge request");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception exception) {
            LOGGER.warn("Concierge request failed");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
