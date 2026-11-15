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

package bt.torrent;

import bt.BtException;
import bt.data.IDataDescriptorFactory;
import bt.data.Storage;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentId;
import bt.service.IRuntimeLifecycleBinder;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple in-memory torrent registry, that creates new descriptors upon request.
 *
 *<p><b>Note that this class implements a service.
 * Hence, is not a part of the public API and is a subject to change.</b></p>
 */
public class AdhocTorrentRegistry implements TorrentRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdhocTorrentRegistry.class);

    private IDataDescriptorFactory dataDescriptorFactory;
    private IRuntimeLifecycleBinder lifecycleBinder;

    private Set<TorrentId> torrentIds;
    private ConcurrentMap<TorrentId, Torrent> torrents;
    private ConcurrentMap<TorrentId, DefaultTorrentDescriptor> descriptors;

    @Inject
    public AdhocTorrentRegistry(IDataDescriptorFactory dataDescriptorFactory,
                                IRuntimeLifecycleBinder lifecycleBinder) {

        this.dataDescriptorFactory = dataDescriptorFactory;
        this.lifecycleBinder = lifecycleBinder;

        this.torrentIds = ConcurrentHashMap.newKeySet();
        this.torrents = new ConcurrentHashMap<>();
        this.descriptors = new ConcurrentHashMap<>();
    }

    @Override
    public Collection<Torrent> getTorrents() {
        return Collections.unmodifiableCollection(torrents.values());
    }

    @Override
    public Collection<TorrentId> getTorrentIds() {
        return Collections.unmodifiableCollection(torrentIds);
    }

    @Override
    public Optional<Torrent> getTorrent(TorrentId torrentId) {
        Objects.requireNonNull(torrentId, "Missing torrent ID");
        return Optional.ofNullable(torrents.get(torrentId));
    }

    @Override
    public Optional<TorrentDescriptor> getDescriptor(TorrentId torrentId) {
        Objects.requireNonNull(torrentId, "Missing torrent ID");
        return Optional.ofNullable(descriptors.get(torrentId));
    }

    @Override
    public synchronized TorrentDescriptor register(Torrent torrent, Storage storage) {
        TorrentId torrentId = torrent.getTorrentId();

        DefaultTorrentDescriptor descriptor = descriptors.get(torrentId);
        if (descriptor != null) {
            synchronized (descriptor) {
                if (descriptor.isActive() && descriptor.getDataDescriptor() != null) {
                    throw new IllegalStateException("Torrent is already active: " + torrent.getTorrentId());
                }
                if (descriptor.getDataDescriptor() != null) {
                    try {
                        descriptor.getDataDescriptor().close();
                    } catch (IOException exception) {
                        throw new BtException("Failed to close previous torrent data descriptor", exception);
                    }
                }
                descriptor.setDataDescriptor(dataDescriptorFactory.createDescriptor(torrent, storage));
                descriptor.start();
            }
        } else {
            descriptor = new DefaultTorrentDescriptor();
            descriptor.setDataDescriptor(dataDescriptorFactory.createDescriptor(torrent, storage));
            descriptor.start();
            descriptors.put(torrentId, descriptor);
            torrentIds.add(torrentId);
            addShutdownHook(torrentId, descriptor);
        }

        torrents.putIfAbsent(torrentId, torrent);
        return descriptor;
    }

    @Override
    public synchronized TorrentDescriptor register(TorrentId torrentId) {
        DefaultTorrentDescriptor existing = descriptors.get(torrentId);
        if (existing != null && existing.isActive()) {
            throw new IllegalStateException("Torrent is already active: " + torrentId);
        }
        if (existing != null) {
            if (existing.getDataDescriptor() != null) {
                try {
                    existing.getDataDescriptor().close();
                } catch (IOException exception) {
                    throw new BtException("Failed to close previous torrent data descriptor", exception);
                }
                existing.setDataDescriptor(null);
            }
            existing.start();
            return existing;
        }

        DefaultTorrentDescriptor descriptor = new DefaultTorrentDescriptor();
        descriptor.start();
        descriptors.put(torrentId, descriptor);
        torrentIds.add(torrentId);
        addShutdownHook(torrentId, descriptor);
        return descriptor;
    }

    @Override
    public boolean isSupportedAndActive(TorrentId torrentId) {
        Optional<TorrentDescriptor> descriptor = getDescriptor(torrentId);
        // it's OK if descriptor is not present -- torrent might be being fetched at the time
        return getTorrentIds().contains(torrentId)
                && (!descriptor.isPresent() || descriptor.get().isActive());
    }

    private void addShutdownHook(TorrentId torrentId, TorrentDescriptor descriptor) {
        lifecycleBinder.onShutdown("Closing data descriptor for torrent ID: " + torrentId, () -> {
            if (descriptor.getDataDescriptor() != null) {
                try {
                    descriptor.getDataDescriptor().close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close data descriptor for torrent ID: {}", torrentId, e);
                }
            }
        });
    }
}
