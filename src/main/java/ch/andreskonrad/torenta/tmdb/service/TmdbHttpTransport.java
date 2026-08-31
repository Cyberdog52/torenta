package ch.andreskonrad.torenta.tmdb.service;

import java.io.IOException;
import java.net.URI;

@FunctionalInterface
public interface TmdbHttpTransport {

    String get(URI uri) throws IOException, InterruptedException;
}
