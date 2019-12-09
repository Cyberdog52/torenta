package ch.andreskonrad.torenta.preference.service;

import org.springframework.stereotype.Service;

import java.util.prefs.Preferences;

@Service
public class PreferenceServiceMock extends PreferenceService {

    private static final String PREFERENCE_NODE_NAME = "ch/andreskonrad/torenta/preferenceMock";

    @Override
    protected Preferences getUserPreferenceRoot() {
        return Preferences.userRoot().node(PREFERENCE_NODE_NAME);
    }
}
