package ch.andreskonrad.torenta.torrent.controller;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.service.TorrentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class TorrentControllerTest {

    @Mock
    private TorrentService torrentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TorrentController(torrentService)).build();
    }

    @Test
    void search_returnsEntriesAndDecodesSearchParameter() throws Exception {
        TorrentEntry entry = new TorrentEntry(
                "Star Wars 1080p",
                "magnet:?xt=urn:btih:abc",
                "/torrent/abc",
                "Today",
                "2 GiB",
                "uploader",
                42,
                3,
                "Video",
                "Movies",
                true,
                true
        );
        when(torrentService.search("Star Wars + & More")).thenReturn(List.of(entry));

        mockMvc.perform(get(URI.create(
                        "/api/torrent?search=Star%20Wars%20%2B%20%26%20More"
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Star Wars 1080p"))
                .andExpect(jsonPath("$[0].magnetLink").value("magnet:?xt=urn:btih:abc"))
                .andExpect(jsonPath("$[0].numberOfSeeders").value(42));

        verify(torrentService).search("Star Wars + & More");
    }

    @Test
    void serviceFailure_returnsBadRequest() throws Exception {
        when(torrentService.search("broken")).thenThrow(new IllegalStateException("failure"));

        mockMvc.perform(get("/api/torrent").queryParam("search", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    @Test
    void missingSearchParameter_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(get("/api/torrent"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(torrentService);
    }
}
