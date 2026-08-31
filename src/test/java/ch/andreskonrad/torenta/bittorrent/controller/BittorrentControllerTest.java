package ch.andreskonrad.torenta.bittorrent.controller;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import ch.andreskonrad.torenta.bittorrent.service.BitTorrentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class BittorrentControllerTest {

    @Mock
    private BitTorrentService bitTorrentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new BittorrentController(bitTorrentService)).build();
    }

    @Test
    void startDownload_bindsJsonAndReturnsOk() throws Exception {
        mockMvc.perform(post("/api/bittorrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "torrentEntry": {
                                    "name": "Days Gone Bye 1080p",
                                    "magnetLink": "magnet:?xt=urn:btih:abc",
                                    "link": "/torrent/abc",
                                    "uploadedTime": "Today",
                                    "size": "2 GiB",
                                    "uploader": "uploader",
                                    "numberOfSeeders": 42,
                                    "numberOfLeechers": 3,
                                    "category": "Video",
                                    "subCategory": "TV",
                                    "uploaderIsVIP": true,
                                    "uploaderIsTrusted": true
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        ArgumentCaptor<DownloadRequest> captor = ArgumentCaptor.forClass(DownloadRequest.class);
        verify(bitTorrentService).startDownloadToPreferredFolder(captor.capture());
        DownloadRequest request = captor.getValue();
        assertNotNull(request.getTorrentEntry());
        assertEquals("Days Gone Bye 1080p", request.getTorrentEntry().getName());
        assertEquals(42, request.getTorrentEntry().getNumberOfSeeders());
    }

    @Test
    void startDownload_serviceFailureReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("failure"))
                .when(bitTorrentService).startDownloadToPreferredFolder(any(DownloadRequest.class));

        mockMvc.perform(post("/api/bittorrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    @Test
    void getTorrents_returnsDownloads() throws Exception {
        DownloadRequest request = new DownloadRequest(null, null, null, null);
        DownloadDto download = new DownloadDto(
                7,
                DownloadState.STARTED,
                0.5,
                request,
                1234L,
                8L,
                4096L,
                512.5,
                null
        );
        when(bitTorrentService.getAllDownloadDtos()).thenReturn(Set.of(download));

        mockMvc.perform(get("/api/bittorrent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].state").value("STARTED"))
                .andExpect(jsonPath("$[0].progress").value(0.5))
                .andExpect(jsonPath("$[0].connectedPeers").value(8))
                .andExpect(jsonPath("$[0].totalBytes").value(4096));
    }

    @Test
    void getTorrents_serviceFailureReturnsInternalServerError() throws Exception {
        when(bitTorrentService.getAllDownloadDtos())
                .thenThrow(new IllegalStateException("failure"));

        mockMvc.perform(get("/api/bittorrent"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
    }

    @Test
    void missingOrMalformedBody_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(post("/api/bittorrent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/bittorrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbEpisode\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bitTorrentService);
    }
}
