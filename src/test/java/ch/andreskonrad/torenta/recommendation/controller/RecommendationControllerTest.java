package ch.andreskonrad.torenta.recommendation.controller;

import ch.andreskonrad.torenta.recommendation.dto.RecommendationResultDto;
import ch.andreskonrad.torenta.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationService recommendationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new RecommendationController(recommendationService)).build();
    }

    @Test
    void get_withoutTrailingSlash_isRoutedCorrectly() throws Exception {
        // Regression test: the controller previously declared its base path with a trailing
        // slash (`/api/recommendation/`) while relying on a bare `@GetMapping` with no
        // sub-path, which only registers the trailing-slash variant on Spring Boot 3+ (strict
        // trailing-slash matching by default) and 404s for the exact path the frontend calls.
        when(recommendationService.getRecommendations(0)).thenReturn(new RecommendationResultDto(0, List.of(), List.of()));

        mockMvc.perform(get("/api/recommendation").param("days", "0"))
                .andExpect(status().isOk());
    }

    @Test
    void get_defaultsDaysToFourteen() throws Exception {
        when(recommendationService.getRecommendations(14)).thenReturn(new RecommendationResultDto(0, List.of(), List.of()));

        mockMvc.perform(get("/api/recommendation"))
                .andExpect(status().isOk());
    }

    @Test
    void get_returnsRecommendationResult() throws Exception {
        when(recommendationService.getRecommendations(3)).thenReturn(new RecommendationResultDto(
                5,
                List.of("Ambiguous Show"),
                List.of()));

        mockMvc.perform(get("/api/recommendation").param("days", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seriesConsidered").value(5))
                .andExpect(jsonPath("$.unresolvedSeriesNames[0]").value("Ambiguous Show"));
    }

    @Test
    void get_serviceFailureReturnsInternalServerError() throws Exception {
        when(recommendationService.getRecommendations(14)).thenThrow(new IllegalStateException("failure"));

        mockMvc.perform(get("/api/recommendation"))
                .andExpect(status().isInternalServerError());
    }
}
