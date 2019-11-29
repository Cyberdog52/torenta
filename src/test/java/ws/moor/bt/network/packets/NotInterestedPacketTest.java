/*
 * BitThief - A Free Riding BitTorrent Client
 * Copyright (C) 2006 Patrick Moor <patrick@moor.ws>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */

package ws.moor.bt.network.packets;
 import org.junit.Test;

import org.easymock.EasyMock;
import ws.moor.bt.util.ByteUtil;
import ws.moor.bt.util.ExtendedTestCase;
import static org.junit.jupiter.api.Assertions.*;
/**
 * TODO(pmoor): Javadoc
 */
public class NotInterestedPacketTest extends ExtendedTestCase {

  @Test
public void testClassConstructor() {
    NotInterestedPacket packet = new NotInterestedPacket();
    NotInterestedPacket packet2 = new NotInterestedPacket();
    assertEquals(packet, packet2);
  }

  @Test
public void testID() {
    assertEquals(3, NotInterestedPacket.ID);
    assertEquals(3, new NotInterestedPacket().getId());
  }

  @Test
public void testEquals() {
    NotInterestedPacket packet = new NotInterestedPacket();
    assertEquals(packet, new NotInterestedPacket());
    assertEquals(new NotInterestedPacket().hashCode(), new NotInterestedPacket().hashCode());
    assertEquals(packet, packet);
  }

  @Test
public void testPayloadLength() {
    assertEquals(0, new NotInterestedPacket().getPayloadLength());
  }

  @Test
public void testConstructor() {
    PacketConstructor<NotInterestedPacket> constructor = NotInterestedPacket.getConstructor();
    assertEquals(3, constructor.getId());
    assertEquals(new NotInterestedPacket(), constructor.constructPacket(new byte[2], 1, 0));
  }

  @Test
public void testWriteInfoBuffer() {
    byte[] buffer = ByteUtil.randomByteArray(16);
    byte[] buffer2 = buffer.clone();
    assertEquals(7, new NotInterestedPacket().writeIntoBuffer(buffer2, 7));
    assertArrayEquals(buffer, buffer2);
  }

  @Test
public void testHandler() {
    NotInterestedPacket packet = new NotInterestedPacket();
    PacketHandler handler = EasyMock.createMock(PacketHandler.class);
    handler.handleNotInterestedPacket(EasyMock.same(packet));
    EasyMock.replay(handler);
    packet.handle(handler);
    EasyMock.verify(handler);
  }
}
