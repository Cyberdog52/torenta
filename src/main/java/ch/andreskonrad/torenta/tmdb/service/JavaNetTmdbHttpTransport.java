package ch.andreskonrad.torenta.tmdb.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class JavaNetTmdbHttpTransport implements TmdbHttpTransport {

    private static final RequestThrottler REQUEST_THROTTLER = new RequestThrottler(9, 1000);

    /**
     * Bounds how long a single TMDB call may take. Without this, a slow or hanging
     * connection (e.g. flaky outbound networking on a CI runner, or TMDB being
     * unreachable) blocks the calling thread indefinitely, since the recommendation
     * scan performs many of these calls sequentially.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;

    public JavaNetTmdbHttpTransport() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
    }

    JavaNetTmdbHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .setHeader("User-Agent", "Java 25 HttpClient Bot")
                .build();

        REQUEST_THROTTLER.throttle();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
