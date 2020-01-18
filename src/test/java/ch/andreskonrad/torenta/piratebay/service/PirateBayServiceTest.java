package ch.andreskonrad.torenta.piratebay.service;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntry;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PirateBayService.class)
@EnableConfigurationProperties
public class PirateBayServiceTest {

    @Autowired
    private PirateBayService piratebayService;

    @Ignore
    @Test
    public void manualSearch() throws PirateBaySearchException {
        ArrayList<PirateBayEntry> entries = piratebayService.search("Walking Dead");

        assertTrue(entries.size() > 0);
    }

}
