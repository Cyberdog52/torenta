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

package ws.moor.bt.util;
 import org.junit.Test;

import org.junit.Before;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;

/**
 * TODO(pmoor): Javadoc
 */
public class ArrayUtilTest extends ExtendedTestCase {

  private byte[] arrayA;

  @Before
  public void setUp() throws Exception {
    arrayA = ByteUtil.randomByteArray(64);
  }

  @Test
public void testResizeArray() {
    byte[] arrayB = ArrayUtil.resize(arrayA, 32);
    byte[] expected = new byte[32];
    System.arraycopy(arrayA, 0, expected, 0, 32);
    assertArrayEquals(expected, arrayB);

    arrayB = ArrayUtil.resize(arrayA, 128);
    expected = new byte[128];
    System.arraycopy(arrayA, 0, expected, 0, 64);
    assertArrayEquals(expected, arrayB);
  }

  @Test
public void testResizeToSameDoesNotCopy() {
    byte[] arrayB = ArrayUtil.resize(arrayA, arrayA.length);
    assertSame(arrayA, arrayB);
  }

  @Test
public void testCutFront() {
    byte[] arrayB = ArrayUtil.cutFront(arrayA, 24);
    byte[] expected = new byte[40];
    System.arraycopy(arrayA, 24, expected, 0, 40);
    assertArrayEquals(expected, arrayB);
  }

  @Test
public void testAppend() {
    byte[] arrayB = ByteUtil.randomByteArray(32);
    byte[] arrayC = ArrayUtil.append(arrayA, arrayB);
    assertEquals(arrayB.length + arrayA.length, arrayC.length);

    byte[] temp = new byte[64];
    System.arraycopy(arrayC, 0, temp, 0, 64);
    assertArrayEquals(arrayA, temp);

    temp = new byte[32];
    System.arraycopy(arrayC, 64, temp, 0, 32);
    assertArrayEquals(arrayB, temp);
  }

  @Test
public void testSubArray() {
    byte[] arrayB = new byte[32];
    System.arraycopy(arrayA, 17, arrayB, 0, 32);
    assertArrayEquals(arrayB, ArrayUtil.subArray(arrayA, 17, 32));
  }

  @Test
public void testSubArrayDoesNotCopyOnSameRange() {
    assertSame(arrayA, ArrayUtil.subArray(arrayA, 0, arrayA.length));
  }
}
