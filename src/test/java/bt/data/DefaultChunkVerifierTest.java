package bt.data;

import bt.data.digest.Digester;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DefaultChunkVerifierTest {

    @Test
    void verify_skipsPiecesRestoredFromValidatedCheckpoint() {
        Digester digester = mock(Digester.class);
        DefaultChunkVerifier verifier = new DefaultChunkVerifier(digester, 1);
        Bitfield bitfield = new Bitfield(2);
        bitfield.markVerified(0);
        bitfield.markVerified(1);

        assertTrue(verifier.verify(
                List.of(mock(ChunkDescriptor.class), mock(ChunkDescriptor.class)),
                bitfield));
        verifyNoInteractions(digester);
    }
}
