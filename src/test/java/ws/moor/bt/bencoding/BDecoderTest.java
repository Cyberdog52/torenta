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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BDecoderTest extends ExtendedTestCase {

    @Test
    public void testStringEquals() {
        assertEquals(new BString("Hello World"), new BString("Hello World"));
    }

    @Test
    public void testUnexpectedEnd() throws ParseException {
        String stringToDecode = "11:Hello";
        try {
            new BDecoder().decode(stringToDecode);
            fail("Should throw an exception");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testStringDecoding() throws IOException, ParseException {
        String stringToDecode = "11:Hello World";
        BString expected = new BString("Hello World");
        assertDecodingCorrect(expected, stringToDecode);
    }

    @Test
    public void testIntegerDecoding() throws IOException, ParseException {
        String stringToDecode = "i42e";
        BInteger expected = new BInteger(42);
        assertDecodingCorrect(expected, stringToDecode);
    }

    @Test
    public void testListDecoding() throws IOException, ParseException {
        String stringToDecode = "li42e11:Hello Worlde";
        BList<BEntity> expected = new BList<BEntity>();
        expected.add(new BInteger(42));
        expected.add(new BString("Hello World"));
        assertDecodingCorrect(expected, stringToDecode);
    }

    @Test
    public void testDictionaryDecoding() throws IOException, ParseException {
        String stringToDecode = "d11:Hello Worldi42e3:Hoii7ee";
        BDictionary<BInteger> expected = new BDictionary<BInteger>();
        expected.put(new BString("Hello World"), new BInteger(42));
        expected.put(new BString("Hoi"), new BInteger(7));
        assertDecodingCorrect(expected, stringToDecode);
    }

    @Test
    public void testMoreComplicatedExample() throws IOException, ParseException {
        String stringToDecode = "ld2:12i144e2:13i169ee5:Halloe";
        BList<BEntity> expected = new BList<BEntity>();
        BDictionary<BEntity> dictionary = new BDictionary<BEntity>();
        dictionary.put(new BString("12"), new BInteger(144));
        dictionary.put(new BString("13"), new BInteger(169));
        expected.add(dictionary);
        expected.add(new BString("Hallo"));
        assertDecodingCorrect(expected, stringToDecode);
    }

    @Test
    public void testInvalidStringDecoding() {
        String stringToDecode = "5a:Hello";
        BDecoder decoder = new BDecoder();
        try {
            decoder.decode(stringToDecode);
            fail();
        } catch (IOException e) {
            fail();
        } catch (ParseException e) {
            // expected
        }
    }

    private void assertDecodingCorrect(BEntity expectedResult, String stringToDecode) throws IOException, ParseException {
        BDecoder decoder = new BDecoder();
        BEntity actual = decoder.decode(stringToDecode);
        assertEquals(expectedResult, actual);
    }
}
