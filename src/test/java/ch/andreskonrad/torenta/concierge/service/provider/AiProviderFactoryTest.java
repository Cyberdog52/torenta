package ch.andreskonrad.torenta.concierge.service.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderFactoryTest {

    @Test
    void selectedProvider_returnsConfiguredProviderCaseInsensitively() {
        AiProvider ollama = mock(AiProvider.class);
        when(ollama.type()).thenReturn(AiProviderType.OLLAMA);

        AiProviderFactory factory = new AiProviderFactory("ollama", List.of(ollama));

        assertEquals(ollama, factory.selectedProvider());
    }

    @Test
    void selectedProvider_rejectsUnavailableProvider() {
        AiProviderFactory factory = new AiProviderFactory("OPENAI", List.of());

        assertThrows(IllegalStateException.class, factory::selectedProvider);
    }

    @Test
    void constructor_rejectsDuplicateProviderTypes() {
        AiProvider first = mock(AiProvider.class);
        AiProvider second = mock(AiProvider.class);
        when(first.type()).thenReturn(AiProviderType.OLLAMA);
        when(second.type()).thenReturn(AiProviderType.OLLAMA);

        assertThrows(
                IllegalStateException.class,
                () -> new AiProviderFactory("OLLAMA", List.of(first, second))
        );
    }
}
