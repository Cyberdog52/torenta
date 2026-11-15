package bt.data.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemStorageUnitTest {

    @TempDir
    Path root;

    @Test
    void closeAndReopen_preservesWrittenBlocks() throws Exception {
        byte[] firstBlock = bytes(0);
        byte[] secondBlock = bytes(16);
        FileSystemStorageUnit storage = new FileSystemStorageUnit(root, "payload.bin", 32);
        storage.writeBlockFully(ByteBuffer.wrap(firstBlock), 0);
        storage.writeBlockFully(ByteBuffer.wrap(secondBlock), 16);
        storage.close();

        FileSystemStorageUnit reopened = new FileSystemStorageUnit(root, "payload.bin", 32);
        ByteBuffer actual = ByteBuffer.allocate(32);
        reopened.readBlockFully(actual, 0);
        reopened.close();

        assertArrayEquals(concat(firstBlock, secondBlock), actual.array());
    }

    @Test
    void writeBlock_rejectsDataPastDeclaredCapacity() {
        FileSystemStorageUnit storage = new FileSystemStorageUnit(root, "payload.bin", 16);

        assertThrows(IllegalArgumentException.class,
                () -> storage.writeBlock(ByteBuffer.wrap(new byte[8]), 12));
    }

    private byte[] bytes(int start) {
        byte[] result = new byte[16];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (start + i);
        }
        return result;
    }

    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
