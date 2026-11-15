/*
 * Copyright (c) 2016—2017 Andrei Tomashpolskiy and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bt.torrent.messaging;

import bt.metainfo.TorrentId;
import bt.net.ConnectionKey;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class PieceIntegrityTrackerTest {

    private final TorrentId torrentId = TorrentId.fromBytes(new byte[TorrentId.length()]);
    private final ConnectionKey firstSource = mock(ConnectionKey.class);
    private final ConnectionKey secondSource = mock(ConnectionKey.class);

    @Test
    public void testAvoidsSoleFailedSourceOnlyWhenAlternativeExists() {
        PieceIntegrityTracker tracker = new PieceIntegrityTracker(torrentId);
        tracker.recordBlock(4, firstSource);

        PieceIntegrityTracker.VerificationFailure failure = tracker.verificationFailed(4);

        assertEquals(1, failure.getFailureCount());
        assertEquals(1, failure.getContributors().size());
        assertTrue(tracker.shouldAvoid(4, firstSource, 2));
        assertFalse(tracker.shouldAvoid(4, firstSource, 1));
        assertFalse(tracker.shouldAvoid(4, secondSource, 2));
    }

    @Test
    public void testDoesNotAttributeMixedSourceFailureToSinglePeer() {
        PieceIntegrityTracker tracker = new PieceIntegrityTracker(torrentId);
        tracker.recordBlock(7, firstSource);
        tracker.recordBlock(7, secondSource);

        PieceIntegrityTracker.VerificationFailure failure = tracker.verificationFailed(7);

        assertEquals(2, failure.getContributors().size());
        assertFalse(tracker.shouldAvoid(7, firstSource, 2));
        assertFalse(tracker.shouldAvoid(7, secondSource, 2));
    }

    @Test
    public void testSuccessfulVerificationClearsPeerPenalty() {
        PieceIntegrityTracker tracker = new PieceIntegrityTracker(torrentId);
        tracker.recordBlock(2, firstSource);
        tracker.verificationFailed(2);

        tracker.verificationSucceeded(2);

        assertFalse(tracker.shouldAvoid(2, firstSource, 2));
    }

    @Test
    public void testWarningsAreAggregatedAndRateLimited() {
        AtomicLong clock = new AtomicLong(1_000);
        PieceIntegrityTracker tracker = new PieceIntegrityTracker(torrentId, clock::get);

        assertFalse(fail(tracker, 0).getSummary().isPresent());
        assertFalse(fail(tracker, 1).getSummary().isPresent());
        PieceIntegrityTracker.FailureSummary firstSummary = fail(tracker, 2).getSummary().orElseThrow();
        assertEquals(3, firstSummary.getFailureCount());
        assertEquals(3, firstSummary.getDistinctPieceCount());

        assertFalse(fail(tracker, 3).getSummary().isPresent());
        assertFalse(fail(tracker, 3).getSummary().isPresent());
        assertFalse(fail(tracker, 3).getSummary().isPresent());

        clock.addAndGet(PieceIntegrityTracker.WARNING_INTERVAL.toMillis());
        PieceIntegrityTracker.FailureSummary secondSummary = fail(tracker, 3).getSummary().orElseThrow();
        assertEquals(4, secondSummary.getFailureCount());
        assertEquals(1, secondSummary.getDistinctPieceCount());
    }

    private PieceIntegrityTracker.VerificationFailure fail(PieceIntegrityTracker tracker, int pieceIndex) {
        tracker.recordBlock(pieceIndex, firstSource);
        return tracker.verificationFailed(pieceIndex);
    }
}
