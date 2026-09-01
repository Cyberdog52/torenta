package ch.andreskonrad.torenta.tmdb.controller;

import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMoviesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class TmdbControllerTest {

    @Mock
    private TmdbService tmdbService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TmdbController(tmdbService)).build();
    }

    @Test
    void searchSeries_returnsResultAndDecodesSearchParameter() throws Exception {
        TmdbSeriesSearchResultDto result = objectMapper.readValue(
                """
                        {"results":[{"id":1402,"name":"The Walking Dead"}]}
                        """,
                TmdbSeriesSearchResultDto.class
        );
        when(tmdbService.searchSeries("The 100% & More")).thenReturn(result);

        mockMvc.perform(get(URI.create(
                        "/api/tmdb/tv?search=The%20100%25%20%26%20More"
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id").value(1402))
                .andExpect(jsonPath("$.results[0].name").value("The Walking Dead"));

        verify(tmdbService).searchSeries("The 100% & More");
    }

    @Test
    void searchMovies_returnsResult() throws Exception {
        TmdbMoviesSearchResultDto result = objectMapper.readValue(
                """
                        {"results":[{"id":550,"title":"Fight Club"}]}
                        """,
                TmdbMoviesSearchResultDto.class
        );
        when(tmdbService.searchMovies("Fight Club")).thenReturn(result);

        mockMvc.perform(get("/api/tmdb/movie")
                        .queryParam("search", "Fight Club"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id").value(550))
                .andExpect(jsonPath("$.results[0].title").value("Fight Club"));
    }

    @Test
    void getTvShow_returnsDetailAndBindsId() throws Exception {
        TmdbSeriesDetailDto detail = objectMapper.readValue(
                """
                        {"id":1402,"name":"The Walking Dead"}
                        """,
                TmdbSeriesDetailDto.class
        );
        when(tmdbService.getSeries(1402)).thenReturn(detail);

        mockMvc.perform(get("/api/tmdb/tv/{id}", 1402))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1402))
                .andExpect(jsonPath("$.name").value("The Walking Dead"));
    }

    @Test
    void getMovie_returnsDetailAndBindsId() throws Exception {
        TmdbMovieDetailDto detail = objectMapper.readValue(
                """
                        {"id":550,"title":"Fight Club"}
                        """,
                TmdbMovieDetailDto.class
        );
        when(tmdbService.getMovie(550)).thenReturn(detail);

        mockMvc.perform(get("/api/tmdb/movie/{id}", 550))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(550))
                .andExpect(jsonPath("$.title").value("Fight Club"));
    }

    @Test
    void getEpisodes_returnsEpisodesAndBindsPathVariables() throws Exception {
        TmdbEpisodeDto episode = objectMapper.readValue(
                """
                        {"id":63056,"episode_number":1,"season_number":1,"name":"Days Gone Bye"}
                        """,
                TmdbEpisodeDto.class
        );
        when(tmdbService.getEpisodes(1402, 1)).thenReturn(new TmdbEpisodeDto[]{episode});

        mockMvc.perform(get("/api/tmdb/tv/{id}/season/{season_number}", 1402, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(63056))
                .andExpect(jsonPath("$[0].episode_number").value(1))
                .andExpect(jsonPath("$[0].name").value("Days Gone Bye"));
    }

    @Test
    void searchEndpoints_returnPreconditionFailedForIllegalStateAndNotFoundForOtherFailures() throws Exception {
        when(tmdbService.searchSeries("broken-series")).thenThrow(new IllegalStateException("failure"));
        when(tmdbService.searchMovies("broken-movie")).thenThrow(new IllegalStateException("failure"));
        when(tmdbService.searchSeries("missing-series")).thenThrow(new RuntimeException("failure"));
        when(tmdbService.searchMovies("missing-movie")).thenThrow(new RuntimeException("failure"));

        mockMvc.perform(get("/api/tmdb/tv").queryParam("search", "broken-series"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/tmdb/movie").queryParam("search", "broken-movie"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/tmdb/tv").queryParam("search", "missing-series"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/tmdb/movie").queryParam("search", "missing-movie"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void detailEndpoints_returnNotFoundForFailures() throws Exception {
        when(tmdbService.getSeries(1402)).thenThrow(new IllegalStateException("failure"));
        when(tmdbService.getMovie(550)).thenThrow(new RuntimeException("failure"));
        when(tmdbService.getEpisodes(1402, 1)).thenThrow(new IllegalStateException("failure"));

        mockMvc.perform(get("/api/tmdb/tv/{id}", 1402))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/tmdb/movie/{id}", 550))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/tmdb/tv/{id}/season/{season_number}", 1402, 1))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void missingSearchParameters_returnBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(get("/api/tmdb/tv"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tmdb/movie"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tmdbService);
    }

    @Test
    void invalidNumericPaths_returnBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(get("/api/tmdb/tv/not-a-number"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tmdb/movie/not-a-number"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tmdb/tv/not-a-number/season/1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tmdb/tv/1402/season/not-a-number"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tmdbService);
    }
}
