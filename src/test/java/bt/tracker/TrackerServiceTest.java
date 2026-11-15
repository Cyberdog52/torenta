package bt.tracker;

import bt.BtException;
import bt.metainfo.TorrentId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TrackerServiceTest {

    @Test
    void getTracker_multiKeyRetainsSupportedTrackersAndTierOrder() {
        List<String> createdUrls = new ArrayList<>();
        TrackerFactory udpFactory = trackerUrl -> {
            createdUrls.add(trackerUrl);
            return successfulTracker();
        };
        TrackerService service = new TrackerService(Map.of("udp", udpFactory));
        AnnounceKey announceKey = new AnnounceKey(List.of(
                List.of("https://unsupported.example/announce", "udp://primary.example:80"),
                List.of("udp://backup.example:80")));

        TrackerResponse response = service.getTracker(announceKey)
                .request(mock(TorrentId.class))
                .query();

        assertTrue(response.isSuccess());
        assertEquals(List.of("udp://primary.example:80"), createdUrls);
    }

    @Test
    void getTracker_multiKeyWithNoSupportedTrackersFailsExplicitly() {
        TrackerService service = new TrackerService(Map.of());
        AnnounceKey announceKey = new AnnounceKey(List.of(
                List.of("https://unsupported.example/announce")));

        assertThrows(BtException.class, () -> service.getTracker(announceKey));
    }

    private Tracker successfulTracker() {
        return torrentId -> new TrackerRequestBuilder(torrentId) {
            @Override
            public TrackerResponse start() {
                return TrackerResponse.ok();
            }

            @Override
            public TrackerResponse stop() {
                return TrackerResponse.ok();
            }

            @Override
            public TrackerResponse complete() {
                return TrackerResponse.ok();
            }

            @Override
            public TrackerResponse query() {
                return TrackerResponse.ok();
            }
        };
    }
}
