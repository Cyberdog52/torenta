package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.service.TorrentSearchException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TorrentParseAPI {

    public static List<TorrentEntry> search(TorrentQuery query) throws TorrentSearchException {

        URI uri = query.getURI();
        String jsonStringResponse = httpGet(uri);
        if (jsonStringResponse == null) {
            return new ArrayList<>();
        }
        try {
            List<PirateBayDto> pirateBayDtoList = Arrays.asList(new ObjectMapper().readValue(jsonStringResponse, PirateBayDto[].class));
            return pirateBayDtoList.stream()
                    .map(PirateBayDto::toTorrentEntry)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    private static String httpGet(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        try {
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
