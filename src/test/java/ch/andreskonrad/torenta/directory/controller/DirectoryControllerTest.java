package ch.andreskonrad.torenta.directory.controller;

import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.dto.FileDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class DirectoryControllerTest {

    @Mock
    private DirectoryService directoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new DirectoryController(directoryService)).build();
    }

    @Test
    void getSeries_returnsDirectoryAndDecodesPath() throws Exception {
        DirectoryDto directory = directory("Star Trek & Beyond");
        when(directoryService.getSeriesDirectory("Star Trek & Beyond")).thenReturn(directory);

        mockMvc.perform(get("/api/directory/series/{seriesTitle}", "Star Trek & Beyond"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Star Trek & Beyond"))
                .andExpect(jsonPath("$.absolutePath").value("/media/Star Trek & Beyond"))
                .andExpect(jsonPath("$.files[0].name").value("episode.mkv"))
                .andExpect(jsonPath("$.directories").isEmpty());

        verify(directoryService).getSeriesDirectory("Star Trek & Beyond");
    }

    @Test
    void getMovie_returnsDirectoryAndBindsReleaseYear() throws Exception {
        DirectoryDto directory = directory("Blade Runner 2049");
        when(directoryService.getMovieDirectory("Blade Runner 2049", 2017)).thenReturn(directory);

        mockMvc.perform(get("/api/directory/movie/{movieTitle}", "Blade Runner 2049")
                        .queryParam("releaseYear", "2017"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blade Runner 2049"))
                .andExpect(jsonPath("$.absolutePath").value("/media/Blade Runner 2049"));

        verify(directoryService).getMovieDirectory("Blade Runner 2049", 2017);
    }

    @Test
    void nullResults_returnNotFound() throws Exception {
        when(directoryService.getSeriesDirectory("Missing Series")).thenReturn(null);
        when(directoryService.getMovieDirectory("Missing Movie", 2020)).thenReturn(null);

        mockMvc.perform(get("/api/directory/series/{seriesTitle}", "Missing Series"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/directory/movie/{movieTitle}", "Missing Movie")
                        .queryParam("releaseYear", "2020"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void serviceFailures_returnInternalServerError() throws Exception {
        when(directoryService.getSeriesDirectory("Broken Series"))
                .thenThrow(new IllegalStateException("failure"));
        when(directoryService.getMovieDirectory("Broken Movie", 2020))
                .thenThrow(new IllegalStateException("failure"));

        mockMvc.perform(get("/api/directory/series/{seriesTitle}", "Broken Series"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/directory/movie/{movieTitle}", "Broken Movie")
                        .queryParam("releaseYear", "2020"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
    }

    @Test
    void missingOrInvalidReleaseYear_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(get("/api/directory/movie/{movieTitle}", "Arrival"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/directory/movie/{movieTitle}", "Arrival")
                        .queryParam("releaseYear", "not-a-year"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(directoryService);
    }

    private DirectoryDto directory(String name) {
        return new DirectoryDto(
                name,
                Set.of(new FileDto("/media/" + name + "/episode.mkv", "episode.mkv")),
                Set.of(),
                "/media/" + name
        );
    }
}
