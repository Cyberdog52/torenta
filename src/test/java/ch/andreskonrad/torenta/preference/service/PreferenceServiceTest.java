package ch.andreskonrad.torenta.preference.service;

import ch.andreskonrad.torenta.preference.dto.UserPreference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PreferenceServiceMock.class)
@EnableConfigurationProperties
public class PreferenceServiceTest {

    @Autowired
    private PreferenceServiceMock preferenceServiceMock;

    @Test
    public void saveDownloadDirectory_customFolder_stored() {
        UserPreference userPreferences = new UserPreference("C:/");

        this.preferenceServiceMock.save(userPreferences);

        UserPreference storedPreferences = this.preferenceServiceMock.loadPreferences();
        assertEquals(userPreferences, storedPreferences);
    }

    @Test
    public void saveDownloadDirectory_null_notStored() {
        UserPreference oldPreferences = new UserPreference("C:/old");
        this.preferenceServiceMock.save(oldPreferences);

        UserPreference newPreferences = new UserPreference(null);
        this.preferenceServiceMock.save(newPreferences);

        UserPreference storedPreferences = this.preferenceServiceMock.loadPreferences();
        assertEquals(oldPreferences, storedPreferences);
    }

}
