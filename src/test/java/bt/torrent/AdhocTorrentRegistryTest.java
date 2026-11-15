package bt.torrent;

import bt.data.DataDescriptor;
import bt.data.IDataDescriptorFactory;
import bt.data.Storage;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentId;
import bt.service.IRuntimeLifecycleBinder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdhocTorrentRegistryTest {

    @Test
    void register_replacesClosedDescriptorDataForManualRestart() throws Exception {
        IDataDescriptorFactory descriptorFactory = mock(IDataDescriptorFactory.class);
        DataDescriptor firstData = mock(DataDescriptor.class);
        DataDescriptor secondData = mock(DataDescriptor.class);
        when(descriptorFactory.createDescriptor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(firstData, secondData);
        AdhocTorrentRegistry registry = new AdhocTorrentRegistry(
                descriptorFactory, mock(IRuntimeLifecycleBinder.class));
        Torrent torrent = torrent();
        TorrentDescriptor first = registry.register(torrent, mock(Storage.class));
        first.stop();

        TorrentDescriptor restarted = registry.register(torrent, mock(Storage.class));

        assertSame(first, restarted);
        assertSame(secondData, restarted.getDataDescriptor());
        verify(firstData).close();
    }

    @Test
    void register_rejectsConcurrentSessionForSameTorrent() {
        IDataDescriptorFactory descriptorFactory = mock(IDataDescriptorFactory.class);
        when(descriptorFactory.createDescriptor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(mock(DataDescriptor.class));
        AdhocTorrentRegistry registry = new AdhocTorrentRegistry(
                descriptorFactory, mock(IRuntimeLifecycleBinder.class));
        Torrent torrent = torrent();
        TorrentDescriptor descriptor = registry.register(torrent, mock(Storage.class));
        descriptor.start();

        assertThrows(IllegalStateException.class,
                () -> registry.register(torrent, mock(Storage.class)));
    }

    @Test
    void register_reservesTorrentBeforeDataDescriptorInitialization() {
        IDataDescriptorFactory descriptorFactory = mock(IDataDescriptorFactory.class);
        DataDescriptor data = mock(DataDescriptor.class);
        when(descriptorFactory.createDescriptor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(data);
        AdhocTorrentRegistry registry = new AdhocTorrentRegistry(
                descriptorFactory, mock(IRuntimeLifecycleBinder.class));
        Torrent torrent = torrent();

        TorrentDescriptor reserved = registry.register(torrent.getTorrentId());

        assertThrows(IllegalStateException.class, () -> registry.register(torrent.getTorrentId()));
        assertSame(reserved, registry.register(torrent, mock(Storage.class)));
        assertSame(data, reserved.getDataDescriptor());
    }

    private Torrent torrent() {
        Torrent torrent = mock(Torrent.class);
        when(torrent.getTorrentId()).thenReturn(TorrentId.fromBytes(new byte[20]));
        return torrent;
    }
}
