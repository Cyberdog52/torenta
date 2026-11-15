package bt.tracker.udp;

import bt.BtException;
import bt.service.IRuntimeLifecycleBinder;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class UdpMessageWorkerTest {

    @Test
    void sendMessage_retriesWithBoundedSocketTimeouts() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (DatagramSocket trackerSocket = new DatagramSocket(new InetSocketAddress(loopback, 0))) {
            trackerSocket.setSoTimeout(500);
            UdpMessageWorker worker = new UdpMessageWorker(
                    new InetSocketAddress(loopback, 0),
                    trackerSocket.getLocalSocketAddress(),
                    mock(IRuntimeLifecycleBinder.class),
                    10,
                    2);
            try {
                assertThrows(BtException.class,
                        () -> worker.sendMessage(new ConnectRequest(), ConnectResponseHandler.handler()));

                int received = 0;
                for (int i = 0; i < 2; i++) {
                    DatagramPacket packet = new DatagramPacket(new byte[64], 64);
                    trackerSocket.receive(packet);
                    received++;
                }
                assertEquals(2, received);
            } finally {
                worker.shutdown();
            }
        }
    }
}
