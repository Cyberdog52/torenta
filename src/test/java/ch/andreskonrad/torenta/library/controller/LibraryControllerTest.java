package ch.andreskonrad.torenta.library.controller;

import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.library.dto.Series;
import ch.andreskonrad.torenta.library.service.LibraryService;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class LibraryControllerTest {

    @Mock
    private LibraryService libraryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new LibraryController(libraryService)).build();
    }

    @Test
    void getSeriesInLibrary_returnsSeriesAndDecodesPath() throws Exception {
        DirectoryDto directory = new DirectoryDto(
                "Schitt's Creek",
                Set.of(),
                Set.of(),
                "/media/Schitt's Creek"
        );
        TmdbSeriesDetailDto detail = objectMapper.readValue(
                """
                        {"id":61662,"name":"Schitt's Creek","seasons":[]}
                        """,
                TmdbSeriesDetailDto.class
        );
        Series series = new Series(
                directory,
                detail,
                Map.<Integer, TmdbEpisodeDto[]>of(),
                new HashMap<>(),
                Set.of()
        );
        when(libraryService.getSeriesInLibrary("Schitt's Creek")).thenReturn(series);

        mockMvc.perform(get("/api/library/tv/{seriesName}", "Schitt's Creek"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directoryDto.name").value("Schitt's Creek"))
                .andExpect(jsonPath("$.seriesDetail.id").value(61662))
                .andExpect(jsonPath("$.seriesDetail.name").value("Schitt's Creek"))
                .andExpect(jsonPath("$.seasonList").isEmpty());

        verify(libraryService).getSeriesInLibrary("Schitt's Creek");
    }

    @Test
    void seriesNotInLibrary_returnsNotFound() throws Exception {
        when(libraryService.getSeriesInLibrary("Not Downloaded")).thenReturn(null);

        mockMvc.perform(get("/api/library/tv/{seriesName}", "Not Downloaded"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void serviceFailure_returnsInternalServerError() throws Exception {
        when(libraryService.getSeriesInLibrary("Broken Series"))
                .thenThrow(new IllegalStateException("failure"));

        mockMvc.perform(get("/api/library/tv/{seriesName}", "Broken Series"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
    }
}
