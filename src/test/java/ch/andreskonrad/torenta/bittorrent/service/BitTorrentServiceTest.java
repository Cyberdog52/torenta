package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitTorrentServiceTest {

    private static final String MAGNET_LINK = "magnet:?xt=urn:btih:abc def";

    private DirectoryService directoryService;
    private BitTorrentClientFactory clientFactory;
    private BtClient client;
    private CompletableFuture<Void> future;
    private BitTorrentService service;

    @BeforeEach
    void setUp() {
        directoryService = mock(DirectoryService.class);
        clientFactory = mock(BitTorrentClientFactory.class);
        client = mock(BtClient.class);
        future = new CompletableFuture<>();
        when(clientFactory.create(any(Config.class), any(Path.class), any(String.class))).thenReturn(client);
        doReturn(future).when(client).startAsync(any(), anyLong());
        service = new BitTorrentService(directoryService, clientFactory);
    }

    @Test
    void startDownloadToPreferredFolder_usesPreferredRootWithoutMediaDetails() {
        Path root = Path.of("preferred");
        when(directoryService.getRootDirectoryPath()).thenReturn(root);
        DownloadRequest request = request(MAGNET_LINK);

        service.startDownloadToPreferredFolder(request);

        verify(clientFactory).create(any(Config.class), eq(root), eq("magnet:?xt=urn:btih:abc%20def"));
        assertSame(root, service.getDownload(MAGNET_LINK.hashCode()).getTargetDirectory());
    }

    @Test
    void startDownloadToPreferredFolder_usesSeriesSeasonDirectory() {
        Path root = Path.of("preferred");
        Path seasonDirectory = Path.of("preferred", "Series", "Show", "S03");
        TmdbSeriesDetailDto series = mock(TmdbSeriesDetailDto.class);
        TmdbEpisodeDto episode = mock(TmdbEpisodeDto.class);
        when(series.getName()).thenReturn("Show");
        when(episode.getSeason_number()).thenReturn(3);
        when(directoryService.getRootDirectoryPath()).thenReturn(root);
        when(directoryService.createDirectoryToSaveSeries("Show", 3)).thenReturn(seasonDirectory);
        DownloadRequest request = request(MAGNET_LINK);
        when(request.getSeriesDetail()).thenReturn(series);
        when(request.getTmdbEpisode()).thenReturn(episode);

        service.startDownloadToPreferredFolder(request);

        verify(directoryService).createDirectoryToSaveSeries("Show", 3);
        verify(clientFactory).create(any(Config.class), eq(seasonDirectory), eq("magnet:?xt=urn:btih:abc%20def"));
    }

    @Test
    void startDownloadToPreferredFolder_usesMovieDirectoryWithValidReleaseYear() {
        Path movieDirectory = Path.of("preferred", "Movies", "Movie (2024)");
        TmdbMovieDetailDto movie = movie("Movie", "2024-06-15");
        when(directoryService.getRootDirectoryPath()).thenReturn(Path.of("preferred"));
        when(directoryService.createDirectoryToSaveMovie("Movie", 2024)).thenReturn(movieDirectory);
        DownloadRequest request = request(MAGNET_LINK);
        when(request.getMovieDetail()).thenReturn(movie);

        service.startDownloadToPreferredFolder(request);

        verify(directoryService).createDirectoryToSaveMovie("Movie", 2024);
        verify(clientFactory).create(any(Config.class), eq(movieDirectory), eq("magnet:?xt=urn:btih:abc%20def"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-a-date", "2024-13-40", "2024"})
    void startDownloadToPreferredFolder_usesMovieDirectoryWithoutYearForMissingOrMalformedReleaseDate(String releaseDate) {
        Path movieDirectory = Path.of("preferred", "Movies", "Movie");
        TmdbMovieDetailDto movie = movie("Movie", releaseDate);
        when(directoryService.getRootDirectoryPath()).thenReturn(Path.of("preferred"));
        when(directoryService.createDirectoryToSaveMovie("Movie", null)).thenReturn(movieDirectory);
        DownloadRequest request = request(MAGNET_LINK);
        when(request.getMovieDetail()).thenReturn(movie);

        service.startDownloadToPreferredFolder(request);

        verify(directoryService).createDirectoryToSaveMovie("Movie", null);
        verify(clientFactory).create(any(Config.class), eq(movieDirectory), eq("magnet:?xt=urn:btih:abc%20def"));
    }

    @Test
    void startDownload_encodesSpacesAndUsesExpectedClientConfiguration() {
        Path target = Path.of("target");
        DownloadRequest request = request(MAGNET_LINK);
        ArgumentCaptor<Config> configCaptor = ArgumentCaptor.forClass(Config.class);

        service.startDownload(request, target);

        verify(clientFactory).create(configCaptor.capture(), eq(target), eq("magnet:?xt=urn:btih:abc%20def"));
        verify(client).startAsync(any(), eq(100L));
        assertEquals(Runtime.getRuntime().availableProcessors() * 2,
                configCaptor.getValue().getNumOfHashingThreads());
    }

    @Test
    void startDownload_rejectsDuplicateMagnetLink() {
        DownloadRequest request = request(MAGNET_LINK);
        service.startDownload(request, Path.of("first"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.startDownload(request, Path.of("second")));

        assertTrue(exception.getMessage().contains(MAGNET_LINK));
        verify(clientFactory, never()).create(any(Config.class), eq(Path.of("second")), any(String.class));
    }

    @Test
    void sessionCallbacks_updateTheirMatchingDownloads() {
        String secondMagnet = "magnet:?xt=urn:btih:second";
        CompletableFuture<Void> secondFuture = new CompletableFuture<>();
        doReturn(future, secondFuture).when(client).startAsync(any(), anyLong());
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();

        service.startDownload(request(MAGNET_LINK), Path.of("first"));
        service.startDownload(request(secondMagnet), Path.of("second"));
        verify(client, org.mockito.Mockito.times(2)).startAsync(callbackCaptor.capture(), eq(100L));
        TorrentSessionState firstState = state(1, 4);
        TorrentSessionState secondState = state(3, 4);

        callbackCaptor.getAllValues().get(0).accept(firstState);

        assertSame(firstState, service.getDownload(MAGNET_LINK.hashCode()).getState());
        assertNull(service.getDownload(secondMagnet.hashCode()).getState());

        callbackCaptor.getAllValues().get(1).accept(secondState);

        assertSame(secondState, service.getDownload(secondMagnet.hashCode()).getState());
    }

    @Test
    void getAllDownloadDtos_collectsEveryDownload() {
        String secondMagnet = "magnet:?xt=urn:btih:second";
        doReturn(future, new CompletableFuture<>()).when(client).startAsync(any(), anyLong());
        ArgumentCaptor<Consumer<TorrentSessionState>> callbackCaptor = sessionCallbackCaptor();
        DownloadRequest firstRequest = request(MAGNET_LINK);
        DownloadRequest secondRequest = request(secondMagnet);
        service.startDownload(firstRequest, Path.of("first"));
        service.startDownload(secondRequest, Path.of("second"));
        verify(client, org.mockito.Mockito.times(2)).startAsync(callbackCaptor.capture(), eq(100L));
        callbackCaptor.getAllValues().get(0).accept(state(1, 4));
        callbackCaptor.getAllValues().get(1).accept(state(2, 4));

        Set<DownloadDto> result = service.getAllDownloadDtos();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getDownloadRequest() == firstRequest));
        assertTrue(result.stream().anyMatch(dto -> dto.getDownloadRequest() == secondRequest));
    }

    @Test
    void downloads_areOwnedByEachServiceInstance() {
        BitTorrentService secondService = new BitTorrentService(directoryService, clientFactory);
        doReturn(future, new CompletableFuture<>()).when(client).startAsync(any(), anyLong());

        service.startDownload(request(MAGNET_LINK), Path.of("first"));
        secondService.startDownload(request(MAGNET_LINK), Path.of("second"));

        assertNotNull(service.getDownload(MAGNET_LINK.hashCode()));
        assertNotNull(secondService.getDownload(MAGNET_LINK.hashCode()));
    }

    private DownloadRequest request(String magnetLink) {
        TorrentEntry torrentEntry = mock(TorrentEntry.class);
        when(torrentEntry.getMagnetLink()).thenReturn(magnetLink);
        DownloadRequest request = mock(DownloadRequest.class);
        when(request.getTorrentEntry()).thenReturn(torrentEntry);
        return request;
    }

    private TmdbMovieDetailDto movie(String title, String releaseDate) {
        TmdbMovieDetailDto movie = mock(TmdbMovieDetailDto.class);
        when(movie.getTitle()).thenReturn(title);
        when(movie.getRelease_date()).thenReturn(releaseDate);
        return movie;
    }

    private TorrentSessionState state(int piecesComplete, int piecesTotal) {
        TorrentSessionState state = mock(TorrentSessionState.class);
        when(state.getPiecesComplete()).thenReturn(piecesComplete);
        when(state.getPiecesTotal()).thenReturn(piecesTotal);
        when(state.getChunksSizeInBytes()).thenReturn(100L);
        when(state.getConnectedPeers()).thenReturn(Set.of());
        when(state.getSaveTimesOfChunks()).thenReturn(List.<LocalDateTime>of());
        return state;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Consumer<TorrentSessionState>> sessionCallbackCaptor() {
        return ArgumentCaptor.forClass(Consumer.class);
    }
}
