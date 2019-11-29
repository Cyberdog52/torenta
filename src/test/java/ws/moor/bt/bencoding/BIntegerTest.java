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

package ws.moor.bt.bencoding;
 import org.junit.Test;

import ws.moor.bt.util.ExtendedTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * TODO(pmoor): Javadoc
 */
public class BIntegerTest extends ExtendedTestCase {

  BInteger a = new BInteger(42);
  BInteger b = new BInteger(17);

  @Test
public void testEquals() {
    BInteger c = new BInteger(42);
    assertEquals(a, c);
  }

  @Test
public void testHashCode() {
    BInteger c = new BInteger(42);
    assertEquals(a.hashCode(), c.hashCode());
  }

  @Test
public void testNotEquals() {
    assertFalse(a.equals(b));
  }

  @Test
public void testConverters() {
    assertEquals((long) 42, a.longValue());
    assertEquals(42, a.intValue());
    assertEquals(42f, a.floatValue(), 0.001);
    assertEquals(42d, a.doubleValue(), 0.001);
  }

  @Test
public void testToString() {
    assertEquals("42", a.toString());
  }

  @Test
public void testEncode() throws IOException {
    assertArrayEquals("i42e".getBytes(), a.encode());
  }

  @Test
public void testParse() throws IOException {
    InputStream source = new ByteArrayInputStream("42e".getBytes());
    PushbackInputStream is = new PushbackInputStream(source);
    assertEquals(a, BInteger.parse(is));
  }
}
