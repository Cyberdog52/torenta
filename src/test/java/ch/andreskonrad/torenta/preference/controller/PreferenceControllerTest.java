package ch.andreskonrad.torenta.preference.controller;

import ch.andreskonrad.torenta.preference.dto.UserPreference;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class PreferenceControllerTest {

    @Mock
    private PreferenceService preferenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new PreferenceController(preferenceService)).build();
    }

    @Test
    void save_bindsJsonAndReturnsOk() throws Exception {
        mockMvc.perform(post("/api/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"downloadDirectoryPath":"/media/downloads"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(preferenceService).save(captor.capture());
        assertEquals("/media/downloads", captor.getValue().getDownloadDirectoryPath());
    }

    @Test
    void save_serviceFailureReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("failure"))
                .when(preferenceService).save(any(UserPreference.class));

        mockMvc.perform(post("/api/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"downloadDirectoryPath":"/invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    @Test
    void get_returnsPreferences() throws Exception {
        when(preferenceService.loadPreferences())
                .thenReturn(new UserPreference("/media/downloads"));

        mockMvc.perform(get("/api/preference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadDirectoryPath").value("/media/downloads"));
    }

    @Test
    void get_serviceFailureReturnsInternalServerError() throws Exception {
        when(preferenceService.loadPreferences())
                .thenThrow(new IllegalStateException("failure"));

        mockMvc.perform(get("/api/preference"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
    }

    @Test
    void missingOrMalformedBody_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(post("/api/preference")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"downloadDirectoryPath\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(preferenceService);
    }
}
