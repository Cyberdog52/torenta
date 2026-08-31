package ch.andreskonrad.torenta.tmdb.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class JavaNetTmdbHttpTransport implements TmdbHttpTransport {

    private static final RequestThrottler REQUEST_THROTTLER = new RequestThrottler(9, 1000);

    private final HttpClient httpClient;

    public JavaNetTmdbHttpTransport() {
        this(HttpClient.newHttpClient());
    }

    JavaNetTmdbHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .setHeader("User-Agent", "Java 25 HttpClient Bot")
                .build();

        REQUEST_THROTTLER.throttle();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
