package bt.torrent;

import bt.metainfo.Torrent;
import bt.tracker.AnnounceKey;
import bt.tracker.ITrackerService;
import bt.tracker.Tracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackerAnnouncerTest {

    @Test
    void constructorDelegatesMixedTierFilteringToTrackerService() {
        ITrackerService trackerService = mock(ITrackerService.class);
        AnnounceKey announceKey = new AnnounceKey(List.of(List.of(
                "https://unsupported.example/announce",
                "udp://supported.example:80")));
        when(trackerService.getTracker(announceKey)).thenReturn(mock(Tracker.class));

        new TrackerAnnouncer(
                trackerService,
                mock(Torrent.class),
                announceKey,
                mock(TorrentSessionState.class));

        verify(trackerService).getTracker(announceKey);
    }
}
