package bt;

import bt.metainfo.TorrentId;
import bt.processor.ProcessingContext;
import bt.processor.Processor;
import bt.processor.listener.ListenerSource;
import bt.runtime.BtRuntime;
import bt.runtime.Config;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultClientTest {

    @Test
    void stop_quiescesTorrentDescriptorBeforeDetachingClient() {
        TorrentId torrentId = TorrentId.fromBytes(new byte[20]);
        ProcessingContext context = mock(ProcessingContext.class);
        when(context.getTorrentId()).thenReturn(Optional.of(torrentId));
        CompletableFuture<Void> processing = new CompletableFuture<>();
        Processor<ProcessingContext> processor = mock(Processor.class);
        doReturn(processing).when(processor).process(
                org.mockito.ArgumentMatchers.eq(context),
                org.mockito.ArgumentMatchers.any());
        TorrentDescriptor descriptor = mock(TorrentDescriptor.class);
        doAnswer(invocation -> {
            processing.complete(null);
            return null;
        }).when(descriptor).stop();
        TorrentRegistry registry = mock(TorrentRegistry.class);
        when(registry.getDescriptor(torrentId)).thenReturn(Optional.of(descriptor));
        BtRuntime runtime = mock(BtRuntime.class);
        when(runtime.isRunning()).thenReturn(true);
        when(runtime.service(TorrentRegistry.class)).thenReturn(registry);
        when(runtime.getConfig()).thenReturn(new Config());
        DefaultClient<ProcessingContext> client = new DefaultClient<>(
                runtime, processor, context, new ListenerSource<>(ProcessingContext.class));

        client.startAsync();
        client.stop();

        verify(descriptor).stop();
        verify(runtime).detachClient(client);
    }

    @Test
    void stop_cancelsProcessingWhenDescriptorStopCannotUnblockCurrentStage() {
        TorrentId torrentId = TorrentId.fromBytes(new byte[20]);
        ProcessingContext context = mock(ProcessingContext.class);
        when(context.getTorrentId()).thenReturn(Optional.of(torrentId));
        CompletableFuture<Void> processing = spy(new CompletableFuture<>());
        Processor<ProcessingContext> processor = mock(Processor.class);
        doReturn(processing).when(processor).process(
                org.mockito.ArgumentMatchers.eq(context),
                org.mockito.ArgumentMatchers.any());
        TorrentDescriptor descriptor = mock(TorrentDescriptor.class);
        TorrentRegistry registry = mock(TorrentRegistry.class);
        when(registry.getDescriptor(torrentId)).thenReturn(Optional.of(descriptor));
        BtRuntime runtime = mock(BtRuntime.class);
        when(runtime.isRunning()).thenReturn(true);
        when(runtime.service(TorrentRegistry.class)).thenReturn(registry);
        when(runtime.getConfig()).thenReturn(new Config());
        DefaultClient<ProcessingContext> client = new DefaultClient<>(
                runtime, processor, context, new ListenerSource<>(ProcessingContext.class));

        client.startAsync();
        client.stop();

        verify(descriptor).stop();
        verify(processing).cancel(true);
        verify(runtime).detachClient(client);
    }
}
