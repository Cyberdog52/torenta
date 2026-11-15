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

package bt.processor.torrent;

import bt.BtException;
import bt.event.EventSink;
import bt.metainfo.TorrentId;
import bt.processor.ContextFinalizer;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
import bt.torrent.TrackerAnnouncer;
import bt.torrent.data.DataWorker;

import java.io.IOException;
import java.util.Optional;

public class TorrentContextFinalizer<C extends TorrentContext> implements ContextFinalizer<C> {

    private TorrentRegistry torrentRegistry;
    private EventSink eventSink;
    private DataWorker dataWorker;

    public TorrentContextFinalizer(TorrentRegistry torrentRegistry, EventSink eventSink, DataWorker dataWorker) {
        this.torrentRegistry = torrentRegistry;
        this.eventSink = eventSink;
        this.dataWorker = dataWorker;
    }

    @Override
    public void finalizeContext(C context) {
        IOException closeFailure = null;
        RuntimeException drainFailure = null;
        Optional<TorrentId> torrentId = context.getTorrentId();
        if (torrentId.isPresent()) {
            TorrentDescriptor descriptor = torrentRegistry.getDescriptor(torrentId.get()).orElse(null);
            if (descriptor != null) {
                descriptor.stop();
            }
            eventSink.fireTorrentStopped(torrentId.get());
            try {
                dataWorker.awaitIdle().join();
            } catch (RuntimeException exception) {
                drainFailure = exception;
            }
            if (descriptor != null) {
                if (descriptor.getDataDescriptor() != null) {
                    try {
                        descriptor.getDataDescriptor().close();
                    } catch (IOException exception) {
                        closeFailure = exception;
                    }
                }
            }
        }
        context.getAnnouncer().ifPresent(TrackerAnnouncer::stop);
        if (closeFailure != null) {
            throw new BtException("Failed to close torrent data", closeFailure);
        }
        if (drainFailure != null) {
            throw new BtException("Failed to drain torrent data operations", drainFailure);
        }
    }
}
