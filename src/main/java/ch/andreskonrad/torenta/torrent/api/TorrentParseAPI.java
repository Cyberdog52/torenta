package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.service.TorrentSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TorrentParseAPI implements TorrentProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentParseAPI.class);

    private final HttpTransport httpTransport;
    private final ObjectMapper objectMapper;

    public TorrentParseAPI() {
        HttpClient httpClient = HttpClient.newHttpClient();
        this.httpTransport = uri -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .setHeader("User-Agent", "Java 25 HttpClient Bot")
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        };
        this.objectMapper = new ObjectMapper();
    }

    TorrentParseAPI(HttpTransport httpTransport) {
        this.httpTransport = httpTransport;
        this.objectMapper = new ObjectMapper();
    }

    public static List<TorrentEntry> search(TorrentQuery query) throws TorrentSearchException {
        return new TorrentParseAPI().find(query);
    }

    @Override
    public List<TorrentEntry> find(TorrentQuery query) throws TorrentSearchException {
        URI uri = query.getPirateBayBackendSearchString();
        String jsonStringResponse;
        try {
            jsonStringResponse = httpTransport.get(uri);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Torrent API request was interrupted");
            return new ArrayList<>();
        } catch (IOException e) {
            LOGGER.warn("Torrent API request failed: {}", e.getMessage());
            return new ArrayList<>();
        }
        if (jsonStringResponse == null) {
            return new ArrayList<>();
        }
        try {
            List<PirateBayDto> pirateBayDtoList = Arrays.asList(objectMapper.readValue(jsonStringResponse, PirateBayDto[].class));
            return pirateBayDtoList.stream()
                    .map(PirateBayDto::toTorrentEntry)
                    .collect(Collectors.toList());
        } catch (JacksonException e) {
            LOGGER.warn("Torrent API returned malformed JSON");
            return new ArrayList<>();
        }
    }

    @FunctionalInterface
    interface HttpTransport {
        String get(URI uri) throws IOException, InterruptedException;
    }
}
