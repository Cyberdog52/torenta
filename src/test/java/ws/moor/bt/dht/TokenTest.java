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

package ws.moor.bt.dht;
 import org.junit.Test;

import org.junit.After;
import org.junit.Before;
import ws.moor.bt.util.AdjustableTimeSource;
import ws.moor.bt.util.ExtendedTestCase;
import ws.moor.bt.util.SystemTimeSource;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TODO(pmoor): Javadoc
 */
public class TokenTest extends ExtendedTestCase {

  private AdjustableTimeSource timeSource;

  @Before
  public void setUp() throws Exception {
    timeSource = new AdjustableTimeSource();
    Token.timeSource = timeSource;
  }

  @After
  public void tearDown() throws Exception {
    Token.timeSource = SystemTimeSource.INSTANCE;
  }

  @Test
public void testForAddress() throws UnknownHostException {
    InetSocketAddress address1 = randomInetSocketAddress();
    InetSocketAddress address2 = randomInetSocketAddress();

    Token token1 = Token.createForAddress(address1);
    Token token2 = Token.createForAddress(address2);

    assertNotEquals(token1, token2);

    assertTrue(Token.isValid(token1, address1));
    assertTrue(Token.isValid(token2, address2));
    assertFalse(Token.isValid(token1, address2));
    assertFalse(Token.isValid(token2, address1));
  }

  @Test
public void testEquals() {
    Token token = Token.createRandom();
    assertEquals(token, token);
    assertNotEquals(Token.createRandom(), token);
  }

  @Test
public void testSecretChanging() {
    InetSocketAddress address = randomInetSocketAddress();

    Token token = Token.createForAddress(address);
    assertTrue(Token.isValid(token, address));

    timeSource.increaseTime(Token.SECRET_REFRESH_INTERVAL + 1);
    Token token2 = Token.createForAddress(address);
    assertTrue(Token.isValid(token, address));
    assertTrue(Token.isValid(token2, address));
    assertNotEquals(token, token2);

    timeSource.increaseTime(Token.SECRET_REFRESH_INTERVAL + 1);
    Token token3 = Token.createForAddress(address);
    assertTrue(Token.isValid(token, address));
    assertTrue(Token.isValid(token2, address));
    assertTrue(Token.isValid(token3, address));
    assertNotEquals(token, token3);
    assertNotEquals(token2, token3);

    timeSource.increaseTime(Token.SECRET_REFRESH_INTERVAL + 1);
    Token token4 = Token.createForAddress(address);
    assertFalse(Token.isValid(token, address));
    assertTrue(Token.isValid(token2, address));
    assertTrue(Token.isValid(token3, address));
    assertTrue(Token.isValid(token4, address));
    assertNotEquals(token2, token4);
    assertNotEquals(token3, token4);
  }
}
