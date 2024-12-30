package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.dto.TorrentSearchBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;

//originally found in https://github.com/anthony-salutari/Java-Pirate-Bay-Api
public class PirateBayHtmlAPI {

    public static ArrayList<TorrentEntry> search(TorrentQuery query) throws HttpServerErrorException {

        Document doc = getDocumentWithRetries(query, 3);

        Elements tableRows = doc.getElementsByTag("tr");

        ArrayList<TorrentEntry> torrentEntries = new ArrayList<>();
        for (Element row : tableRows) {
            if (!row.hasClass("header")) {
                try {
                    torrentEntries.add(parsePiratebayEntry(row));
                } catch (Exception e) {
                    //  System.out.println("Could not parse element " + row.toString());
                }
            }
        }

        return torrentEntries;
    }

    private static Document getDocumentWithRetries(TorrentQuery query, int retriesLeft) throws HttpServerErrorException {
        if (retriesLeft < 0) {
            throw new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Could not connect to piratebay");
        }
        try {
            return Jsoup.connect(query.getPirateBayFrontendSearchURI().toString())
                    .userAgent("Java 11 HttpClient Bot")
                    .timeout(5000)
                    .get();
        } catch (Exception e) {
            return getDocumentWithRetries(query, retriesLeft - 1);
        }
    }

    private static TorrentEntry parsePiratebayEntry(Element element) {
        final TorrentSearchBuilder builder = new TorrentSearchBuilder();

        Element td1 = element.children().select("td").first();
        ArrayList<Element> categories = td1.children().select("a");
        builder.setCategory(categories.get(0).text());

        Element td2 = element.children().select("td").get(1);
        Element aTorrentName = td2.children().select("a").first();
        builder.setName(aTorrentName.text());

        Element td3 = element.children().select("td").get(2);
        builder.setUploadedTime(td3.text());

        Element td4 = element.children().select("td").get(3);
        Element magnetLink = td4.children().select("a").first();
        builder.setMagnetLink(magnetLink.attr("href"));

        ArrayList<Element> icons = td4.children().select("img");
        for (Element icon : icons) {
            String attribute = icon.attr("alt");

            if (attribute.equals("VIP")) {
                builder.setUploaderIsVIP(true);
            }
            if (attribute.equals("Trusted")) {
                builder.setUploaderIsTrusted(true);
            }
        }

        Element td5 = element.children().select("td").get(4);
        builder.setSize(td5.text());

        Element td6 = element.children().select("td").get(5);
        builder.setNumberOfSeeders(Integer.parseInt(td6.text()));

        Element td7 = element.children().select("td").get(6);
        builder.setNumberOfLeechers(Integer.parseInt(td7.text()));

        Element td8 = element.children().select("td").get(7);
        builder.setUploader(td8.text());

        return builder.createTorrentEntry();
    }
}
