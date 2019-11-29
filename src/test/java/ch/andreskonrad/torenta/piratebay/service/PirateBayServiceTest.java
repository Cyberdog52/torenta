package ch.andreskonrad.torenta.piratebay.service;

import piratebay.dto.PirateBayEntry;
import piratebay.service.PirateBayService;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

public class PirateBayServiceTest {

    //TODO make autowired
    private PirateBayService piratebayService = new PirateBayService();

    @Ignore
    @Test
    public void manualSearch() {
        ArrayList<PirateBayEntry> entries = piratebayService.search("Walking Dead");

        assertTrue(entries.size() > 0);
    }

}
