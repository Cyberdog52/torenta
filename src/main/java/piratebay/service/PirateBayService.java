package piratebay.service;

import piratebay.api.PirateBayAPI;
import piratebay.api.PirateBayCategory;
import piratebay.api.PirateBayQuery;
import piratebay.api.PirateBayQueryOrder;
import piratebay.dto.PirateBayEntry;

import java.util.ArrayList;

public class PirateBayService {

    public ArrayList<PirateBayEntry> search(String searchString) {
        try {
            PirateBayQuery query = new PirateBayQuery(searchString, 1, PirateBayCategory.TVshows, PirateBayQueryOrder.ByDefault);
            return PirateBayAPI.Search(query);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
