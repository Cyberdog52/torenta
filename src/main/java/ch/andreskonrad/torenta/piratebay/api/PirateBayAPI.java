package ch.andreskonrad.torenta.piratebay.api;

import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntry;
import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntryBuilder;
import ch.andreskonrad.torenta.piratebay.service.PirateBaySearchException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.stream.Collectors;

//originally found in https://github.com/anthony-salutari/Java-Pirate-Bay-Api
public class PirateBayAPI {

	public static ArrayList<PirateBayEntry> search(PirateBayQuery query) throws PirateBaySearchException {

		Document doc = getDocumentWithRetries(query, 3);

		Elements tableRows = doc.getElementsByTag("tr");

		return tableRows.stream()
				.filter(element -> !element.hasClass("header"))
				.map(PirateBayAPI::parsePiratebayEntry)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private static Document getDocumentWithRetries(PirateBayQuery query, int retriesLeft) throws PirateBaySearchException {
		if (retriesLeft < 0) {
			throw new PirateBaySearchException("Could not connect to piratebay");
		}
		try {
			return Jsoup.connect(query.TranslateToUrl())
					.userAgent(PirateBayConstants.UserAgent)
					.timeout(5000)
					.get();
		} catch (Exception e ) {
			return getDocumentWithRetries(query, retriesLeft - 1);
		}
	}

	private static PirateBayEntry parsePiratebayEntry(Element element) {
		final PirateBayEntryBuilder builder = new PirateBayEntryBuilder();

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
		return builder.createPiratebayEntry();
	}
}
