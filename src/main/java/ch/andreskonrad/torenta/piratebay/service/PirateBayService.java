package ch.andreskonrad.torenta.piratebay.service;

import ch.andreskonrad.torenta.piratebay.api.PirateBayAPI;
import ch.andreskonrad.torenta.piratebay.api.PirateBayCategory;
import ch.andreskonrad.torenta.piratebay.api.PirateBayQuery;
import ch.andreskonrad.torenta.piratebay.api.PirateBayQueryOrder;
import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class PirateBayService {

    public ArrayList<PirateBayEntry> search(String searchString) {
        try {
            PirateBayQuery query = new PirateBayQuery(searchString, 0, PirateBayCategory.All, PirateBayQueryOrder.ByDefault);
            return PirateBayAPI.search(query);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
