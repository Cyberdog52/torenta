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

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;

public class PieceIntegrityTracker {

    static final int WARNING_THRESHOLD = 3;
    static final Duration WARNING_INTERVAL = Duration.ofMinutes(1);

    private final TorrentId torrentId;
    private final LongSupplier clock;
    private final Map<Integer, PieceAttempt> attempts;
    private final Set<Integer> failedPiecesSinceWarning;

    private int failuresSinceWarning;
    private long lastWarningAt = -1;

    public PieceIntegrityTracker(TorrentId torrentId) {
        this(torrentId, System::currentTimeMillis);
    }

    PieceIntegrityTracker(TorrentId torrentId, LongSupplier clock) {
        this.torrentId = torrentId;
        this.clock = clock;
        this.attempts = new HashMap<>();
        this.failedPiecesSinceWarning = new HashSet<>();
    }

    synchronized void recordBlock(int pieceIndex, ConnectionKey source) {
        attempts.computeIfAbsent(pieceIndex, ignored -> new PieceAttempt()).contributors.add(source);
    }

    synchronized VerificationFailure verificationFailed(int pieceIndex) {
        PieceAttempt attempt = attempts.computeIfAbsent(pieceIndex, ignored -> new PieceAttempt());
        Set<ConnectionKey> contributors = Set.copyOf(attempt.contributors);
        attempt.contributors.clear();
        attempt.failureCount++;
        attempt.avoidedSource = contributors.size() == 1 ? contributors.iterator().next() : null;

        failuresSinceWarning++;
        failedPiecesSinceWarning.add(pieceIndex);

        Optional<FailureSummary> summary = Optional.empty();
        long now = clock.getAsLong();
        if (failuresSinceWarning >= WARNING_THRESHOLD
                && (lastWarningAt < 0 || now - lastWarningAt >= WARNING_INTERVAL.toMillis())) {
            summary = Optional.of(new FailureSummary(
                    torrentId, failuresSinceWarning, failedPiecesSinceWarning.size()));
            failuresSinceWarning = 0;
            failedPiecesSinceWarning.clear();
            lastWarningAt = now;
        }

        return new VerificationFailure(torrentId, pieceIndex, attempt.failureCount, contributors, summary);
    }

    synchronized void verificationSucceeded(int pieceIndex) {
        attempts.remove(pieceIndex);
    }

    synchronized void verificationAborted(int pieceIndex) {
        PieceAttempt attempt = attempts.get(pieceIndex);
        if (attempt != null) {
            attempt.contributors.clear();
        }
    }

    synchronized boolean shouldAvoid(int pieceIndex, ConnectionKey source, int availableSources) {
        PieceAttempt attempt = attempts.get(pieceIndex);
        return availableSources > 1 && attempt != null && source.equals(attempt.avoidedSource);
    }

    static final class VerificationFailure {
        private final TorrentId torrentId;
        private final int pieceIndex;
        private final int failureCount;
        private final Set<ConnectionKey> contributors;
        private final Optional<FailureSummary> summary;

        private VerificationFailure(TorrentId torrentId,
                                    int pieceIndex,
                                    int failureCount,
                                    Set<ConnectionKey> contributors,
                                    Optional<FailureSummary> summary) {
            this.torrentId = torrentId;
            this.pieceIndex = pieceIndex;
            this.failureCount = failureCount;
            this.contributors = contributors;
            this.summary = summary;
        }

        TorrentId getTorrentId() {
            return torrentId;
        }

        int getPieceIndex() {
            return pieceIndex;
        }

        int getFailureCount() {
            return failureCount;
        }

        Set<ConnectionKey> getContributors() {
            return contributors;
        }

        Optional<FailureSummary> getSummary() {
            return summary;
        }
    }

    static final class FailureSummary {
        private final TorrentId torrentId;
        private final int failureCount;
        private final int distinctPieceCount;

        private FailureSummary(TorrentId torrentId, int failureCount, int distinctPieceCount) {
            this.torrentId = torrentId;
            this.failureCount = failureCount;
            this.distinctPieceCount = distinctPieceCount;
        }

        TorrentId getTorrentId() {
            return torrentId;
        }

        int getFailureCount() {
            return failureCount;
        }

        int getDistinctPieceCount() {
            return distinctPieceCount;
        }
    }

    private static final class PieceAttempt {
        private final Set<ConnectionKey> contributors = new HashSet<>();
        private int failureCount;
        private ConnectionKey avoidedSource;
    }
}
