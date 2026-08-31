package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PirateBayDtoTest {

    @Test
    void toTorrentEntryMapsFieldsAndEncodesMagnetName() {
        PirateBayDto dto = dto(7, 2_048);
        dto.setName("Rock & Röll/Live");
        dto.setInfo_hash("ABC123");
        dto.setLeechers(12);
        dto.setSeeders(34);
        dto.setUsername("uploader");
        dto.setAdded(0);
        dto.setStatus("ViP");

        TorrentEntry entry = dto.toTorrentEntry();

        assertEquals("Rock & Röll/Live", entry.getName());
        assertEquals(12, entry.getNumberOfLeechers());
        assertEquals(34, entry.getNumberOfSeeders());
        assertEquals("uploader", entry.getUploader());
        assertEquals("1970-01-01T00:00:00Z", entry.getUploadedTime());
        assertEquals(" 2.00 KB", entry.getSize());
        assertTrue(entry.isUploaderIsVIP());
        assertFalse(entry.isUploaderIsTrusted());
        assertTrue(entry.getMagnetLink().startsWith(
                "magnet:?xt=urn%3Abtih%3AABC123&dn=Rock+%26+R%C3%B6ll%2FLive&tr="));
    }

    @Test
    void nonVipAndMissingStatusAreNotVip() {
        PirateBayDto dto = dto(1, 0);
        dto.setStatus("trusted");
        assertFalse(dto.toTorrentEntry().isUploaderIsVIP());

        dto.setStatus(null);
        assertFalse(dto.toTorrentEntry().isUploaderIsVIP());
    }

    @Test
    void exactBinaryUnitBoundariesUseTheLargerUnit() {
        assertEquals(" 1023.00 Byte", dto(1, 1_023).toTorrentEntry().getSize());
        assertEquals(" 1.00 KB", dto(1, 1_024).toTorrentEntry().getSize());
        assertEquals(" 1.00 MB", dto(1, 1_048_576).toTorrentEntry().getSize());
        assertEquals(" 1.00 GB", dto(1, 1_073_741_824).toTorrentEntry().getSize());
    }

    @Test
    void equalityAndHashCodeDependOnlyOnId() {
        PirateBayDto first = dto(42, 1);
        first.setName("first");
        PirateBayDto sameId = dto(42, 2);
        sameId.setName("second");
        PirateBayDto otherId = dto(43, 1);

        assertEquals(first, first);
        assertEquals(first, sameId);
        assertEquals(first.hashCode(), sameId.hashCode());
        assertNotEquals(first, otherId);
        assertNotEquals(first, null);
        assertNotEquals(first, "42");
    }

    private PirateBayDto dto(long id, long size) {
        return new PirateBayDto(id, "name", "HASH", 0, 0, 1, size, "user", 0, "member", 200, "");
    }
}
