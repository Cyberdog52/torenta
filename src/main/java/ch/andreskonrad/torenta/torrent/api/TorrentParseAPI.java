package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.dto.TorrentSearchBuilder;
import ch.andreskonrad.torenta.torrent.service.TorrentSearchException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

public class TorrentParseAPI {

	public static ArrayList<TorrentEntry> search(TorrentQuery query) throws TorrentSearchException {

		Document doc = getDocumentWithRetries(query, 3);

		Elements tableRows = doc.getElementsByTag("tr");

		ArrayList<TorrentEntry> torrentEntries = tableRows.stream()
				.filter(element -> !element.hasClass("header"))
				.filter(element -> element.children().size() == 8)
				.map(TorrentParseAPI::parseTorrentEntry)
				.collect(Collectors.toCollection(ArrayList::new));
		torrentEntries.removeIf(Objects::isNull);
		return torrentEntries;
	}

	private static Document getDocumentWithRetries(TorrentQuery query, int retriesLeft) throws TorrentSearchException {
		if (retriesLeft < 0) {
			throw new TorrentSearchException("Could not connect to " + query.TranslateToUrl());
		}
		try {
			return Jsoup.connect(query.TranslateToUrl())
					.userAgent(TorrentSearchConstants.UserAgent)
					.timeout(5000)
					.get();
		} catch (Exception e ) {
			return getDocumentWithRetries(query, retriesLeft - 1);
		}
	}

	private static TorrentEntry parseTorrentEntry(Element element) {
		try {
			final TorrentSearchBuilder builder = new TorrentSearchBuilder();

			builder.setMagnetLink(element.children().select("td").get(0).children().select("a").first().attr("href"));
			builder.setName(element.children().select("td").get(1).children().select("a").first().attr("title"));
			builder.setUploadedTime(element.children().select("td").get(2).text());
			builder.setSize(element.children().select("td").get(5).text());
			builder.setNumberOfSeeders(Integer.parseInt(element.children().select("td").get(6).text()));
			builder.setNumberOfLeechers(Integer.parseInt(element.children().select("td").get(7).text()));

			return builder.createTorrentEntry();
		} catch (Exception e) {
			return null;
		}

	}
}
