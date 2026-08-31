package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.dto.TorrentSearchBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

//originally found in https://github.com/anthony-salutari/Java-Pirate-Bay-Api
@Component
@Primary
public class PirateBayHtmlAPI implements TorrentProvider {

    private static final int MAX_ATTEMPTS = 4;

    private final DocumentFetcher documentFetcher;

    public PirateBayHtmlAPI() {
        this(uri -> Jsoup.connect(uri.toString())
                .userAgent("Java 25 HttpClient Bot")
                .timeout(5000)
                .get());
    }

    PirateBayHtmlAPI(DocumentFetcher documentFetcher) {
        this.documentFetcher = documentFetcher;
    }

    public static ArrayList<TorrentEntry> search(TorrentQuery query) throws HttpServerErrorException {
        return new PirateBayHtmlAPI().findEntries(query);
    }

    @Override
    public List<TorrentEntry> find(TorrentQuery query) throws HttpServerErrorException {
        return findEntries(query);
    }

    private ArrayList<TorrentEntry> findEntries(TorrentQuery query) throws HttpServerErrorException {
        Document doc = getDocumentWithRetries(query, MAX_ATTEMPTS);

        Elements tableRows = doc.getElementsByTag("tr");

        ArrayList<TorrentEntry> torrentEntries = new ArrayList<>();
        for (Element row : tableRows) {
            if (!row.hasClass("header")) {
                try {
                    torrentEntries.add(parsePiratebayEntry(row));
                } catch (RuntimeException ignored) {
                    // Malformed result rows do not invalidate the other search results.
                }
            }
        }

        return torrentEntries;
    }

    private Document getDocumentWithRetries(TorrentQuery query, int maxAttempts) throws HttpServerErrorException {
        URI uri = query.getPirateBayFrontendSearchURI();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return documentFetcher.fetch(uri);
            } catch (IOException ignored) {
            }
        }
        throw new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Could not connect to piratebay");
    }

    private TorrentEntry parsePiratebayEntry(Element element) {
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

    @FunctionalInterface
    interface DocumentFetcher {
        Document fetch(URI uri) throws IOException;
    }
}
