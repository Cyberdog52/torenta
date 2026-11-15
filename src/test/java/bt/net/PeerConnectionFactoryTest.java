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

package bt.net;

import bt.event.EventSource;
import bt.net.buffer.BorrowedBuffer;
import bt.net.buffer.IBufferManager;
import bt.net.crypto.MSEHandshakeProcessor;
import bt.net.pipeline.ChannelHandlerContext;
import bt.net.pipeline.ChannelPipeline;
import bt.net.pipeline.ChannelPipelineBuilder;
import bt.net.pipeline.IChannelPipelineFactory;
import bt.protocol.Message;
import bt.protocol.crypto.EncryptionPolicy;
import bt.protocol.handler.MessageHandler;
import bt.runtime.Config;
import bt.torrent.TorrentRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeerConnectionFactoryTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisteredHandlerReleasesBuffersAfterConnectionFailure() throws IOException {
        Selector selector = mock(Selector.class);
        IConnectionHandlerFactory connectionHandlerFactory = mock(IConnectionHandlerFactory.class);
        IChannelPipelineFactory pipelineFactory = mock(IChannelPipelineFactory.class);
        MessageHandler<Message> protocol = mock(MessageHandler.class);
        TorrentRegistry torrentRegistry = mock(TorrentRegistry.class);
        IBufferManager bufferManager = mock(IBufferManager.class);
        DataReceiver dataReceiver = mock(DataReceiver.class);
        EventSource eventSource = mock(EventSource.class);
        Config config = mock(Config.class);
        when(config.getAcceptorAddress()).thenReturn(InetAddress.getLoopbackAddress());
        when(config.getEncryptionPolicy()).thenReturn(EncryptionPolicy.PREFER_PLAINTEXT);
        when(config.getMsePrivateKeySize()).thenReturn(128);

        BorrowedBuffer<ByteBuffer> inboundBuffer = mock(BorrowedBuffer.class);
        BorrowedBuffer<ByteBuffer> outboundBuffer = mock(BorrowedBuffer.class);
        when(bufferManager.borrowByteBuffer()).thenReturn(inboundBuffer, outboundBuffer);
        when(inboundBuffer.lockAndGet()).thenReturn(ByteBuffer.allocate(32));
        when(outboundBuffer.lockAndGet()).thenReturn(ByteBuffer.allocate(32));

        ChannelPipelineBuilder pipelineBuilder = mock(ChannelPipelineBuilder.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        when(pipelineFactory.buildPipeline(any())).thenReturn(pipelineBuilder);
        when(pipelineBuilder.channel(any())).thenReturn(pipelineBuilder);
        when(pipelineBuilder.protocol(any())).thenReturn(pipelineBuilder);
        when(pipelineBuilder.inboundBuffer(any())).thenReturn(pipelineBuilder);
        when(pipelineBuilder.outboundBuffer(any())).thenReturn(pipelineBuilder);
        when(pipelineBuilder.build()).thenReturn(pipeline);
        when(pipeline.bindHandler(any())).thenReturn(context);

        AtomicBoolean registered = new AtomicBoolean();
        AtomicBoolean inboundReleased = new AtomicBoolean();
        AtomicBoolean outboundReleased = new AtomicBoolean();
        AtomicInteger releasedBuffers = new AtomicInteger();
        doAnswer(invocation -> {
            registered.set(true);
            return null;
        }).when(dataReceiver).registerChannel(any(), any());
        doAnswer(invocation -> {
            registered.set(false);
            return null;
        }).when(dataReceiver).unregisterChannel(any());
        doAnswer(invocation -> {
            if (!registered.get() && inboundReleased.compareAndSet(false, true)) {
                releasedBuffers.incrementAndGet();
            }
            return null;
        }).when(inboundBuffer).release();
        doAnswer(invocation -> {
            if (!registered.get() && outboundReleased.compareAndSet(false, true)) {
                releasedBuffers.incrementAndGet();
            }
            return null;
        }).when(outboundBuffer).release();

        PeerConnectionFactory factory = new PeerConnectionFactory(
                selector, connectionHandlerFactory, pipelineFactory, protocol, torrentRegistry,
                bufferManager, dataReceiver, eventSource, config);
        MSEHandshakeProcessor handshakeProcessor = mock(MSEHandshakeProcessor.class);
        when(handshakeProcessor.negotiateIncoming(any(), any(), any(), any())).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(factory, "cryptoHandshakeProcessor", handshakeProcessor);

        SocketChannel channel = mock(SocketChannel.class);
        when(channel.getRemoteAddress()).thenThrow(new IOException("Connection closed"));

        ConnectionResult result = factory.createIncomingConnection(mock(Peer.class), channel);

        assertFalse(result.isSuccess());
        assertEquals(2, releasedBuffers.get());
        verify(dataReceiver).unregisterChannel(channel);
        verify(channel).close();
    }
}
