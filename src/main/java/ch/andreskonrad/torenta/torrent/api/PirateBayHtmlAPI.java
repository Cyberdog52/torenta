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
        } catch (Exception e ) {
            return getDocumentWithRetries(query, retriesLeft - 1);
        }
    }

    private static TorrentEntry parsePiratebayEntry(Element element) {
        final TorrentSearchBuilder builder = new TorrentSearchBuilder();

        //TODO: refactor some more
        Element td1 = element.children().select("td").first();
        ArrayList<Element> categories = td1.children().select("a");
        builder.setCategory(categories.get(0).text())
                .setSubCategory(categories.get(1).text());

        Element td2 = element.children().select("td").get(1);
        Element aTorrentName = td2.children().select("a").first();
        Element torrentMagnet = td2.children().select("a").get(1);
        builder.setName(aTorrentName.text())
                .setLink(aTorrentName.attr("href"))
                .setMagnetLink(torrentMagnet.attr("href"));

        ArrayList<Element> icons = td2.children().select("img");
        for (Element icon : icons) {
            String attribute = icon.attr("alt");

            if (attribute.equals("VIP")) {
                builder.setUploaderIsVIP(true);
            }
            if (attribute.equals("Trusted")) {
                builder.setUploaderIsTrusted(true);
            }
        }

        Element details = td2.select("font").first();
        String[] uploadInfos = details.text().split(",");

        builder.setUploadedTime(uploadInfos[0].substring(9))
                .setSize(uploadInfos[1].substring(6))
                .setUploader(uploadInfos[2].substring(9));

        Element td3 = element.children().select("td").get(2);
        builder.setNumberOfSeeders(Integer.parseInt(td3.text()));

        try {
            Element td4 = element.children().select("td").get(3);
            builder.setNumberOfLeechers(Integer.parseInt(td4.text()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return builder.createTorrentEntry();
    }
}
