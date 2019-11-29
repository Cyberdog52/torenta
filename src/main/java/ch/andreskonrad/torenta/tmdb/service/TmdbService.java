package ch.andreskonrad.torenta.tmdb.service;

import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;
import ch.andreskonrad.torenta.tmdb.dto.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class TmdbService {

    //TODO
    private static String apiKey = "554b9c38cb52072b92d7b06179f2752e";
    private final String host = "api.themoviedb.org";
    private final String scheme = "https";

    private static HttpClient httpClient = HttpClient.newHttpClient();

    public SearchResult search(String searchString) {
        String jsonStringResponse = searchRequest(searchString);
        try {
            return new ObjectMapper().readValue(jsonStringResponse, SearchResult.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public SeriesDetail get(int id) {
        String jsonStringResponse = detailRequest(id);
        try {
            return new ObjectMapper().readValue(jsonStringResponse, SeriesDetail.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String detailRequest(int id) {
        return httpGet(getDefaultComponentsBuilder()
                .path("/3/tv/" + String.valueOf(id))
                .build()
                .encode()
                .toUri());
    }

    private String searchRequest(String searchString) {
        return httpGet(getDefaultComponentsBuilder()
                .path("/3/search/tv")
                .queryParam("query", searchString.toLowerCase())
                .queryParam("page", "1")
                .build()
                .encode()
                .toUri());
    }

    private UriComponentsBuilder getDefaultComponentsBuilder() {
        return UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host(host)
                .queryParam("api_key", apiKey)
                .queryParam("language", "en-US");
    }

    private String httpGet(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            return null;
        }
    }
}
