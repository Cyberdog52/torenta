package ch.andreskonrad.torenta.recommendation.controller;

import ch.andreskonrad.torenta.recommendation.dto.RecommendationResultDto;
import ch.andreskonrad.torenta.recommendation.service.RecommendationService;
import ch.andreskonrad.torenta.tmdb.service.MissingTmdbKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<RecommendationResultDto> getRecommendations(
            @RequestParam(name = "days", defaultValue = "" + RecommendationService.DEFAULT_DAYS_BACK) int days) {
        RecommendationResultDto recommendations;
        try {
            recommendations = recommendationService.getRecommendations(days);
        } catch (MissingTmdbKeyException exception) {
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(recommendations, HttpStatus.OK);
    }
}
