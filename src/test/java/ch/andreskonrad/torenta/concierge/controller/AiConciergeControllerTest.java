package ch.andreskonrad.torenta.concierge.controller;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.concierge.service.AiConciergeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AiConciergeControllerTest {

    @Mock
    private AiConciergeService conciergeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new AiConciergeController(conciergeService)).build();
    }

    @Test
    void search_returnsRankedResponse() throws Exception {
        SearchIntent intent = new SearchIntent(
                AiMediaType.MOVIE, List.of("FUNNY"), null,
                List.of(new NumericFilterCriterion(
                        NumericFilterKey.RUNTIME, FilterOperator.LTE, 120.0, "under 120 minutes"
                )),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(conciergeService.search("something funny")).thenReturn(new AiConciergeResponse(
                intent,
                List.of(new AiRankedResult(
                        1, AiMediaType.MOVIE, 1, "Funny Movie", "Overview",
                        null, "2020-01-01", 8.0, "Fits your mood"
                ))
        ));

        mockMvc.perform(post("/api/concierge/search")
                        .contentType("application/json")
                        .content("""
                                {"prompt":"something funny"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent.numericFilters[0].value").value(120))
                .andExpect(jsonPath("$.results[0].title").value("Funny Movie"))
                .andExpect(jsonPath("$.results[0].explanation").value("Fits your mood"));
    }

    @Test
    void search_mapsValidationAndDownstreamFailures() throws Exception {
        when(conciergeService.search("")).thenThrow(new IllegalArgumentException("invalid"));
        when(conciergeService.search("offline")).thenThrow(new IllegalStateException("offline"));

        mockMvc.perform(post("/api/concierge/search")
                        .contentType("application/json")
                        .content("""
                                {"prompt":""}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/concierge/search")
                        .contentType("application/json")
                        .content("""
                                {"prompt":"offline"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_rejectsMissingOrMalformedBody() throws Exception {
        when(conciergeService.search(null)).thenThrow(new IllegalArgumentException("invalid"));

        mockMvc.perform(post("/api/concierge/search")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/concierge/search")
                        .contentType("application/json")
                        .content("null"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/concierge/search")
                        .contentType("application/json")
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }
}
