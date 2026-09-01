package ch.andreskonrad.torenta.concierge.service.provider;

import ch.andreskonrad.torenta.preference.dto.UserPreference;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiAiProviderTest {

    @Test
    void chatClient_usesApiKeyFromUserPreferences() {
        PreferenceService preferenceService = mock(PreferenceService.class);
        when(preferenceService.loadPreferences())
                .thenReturn(new UserPreference(null, null, "openai-key"));
        OpenAiAiProvider provider = new OpenAiAiProvider(
                preferenceService,
                new ObjectMapper(),
                "https://api.openai.com",
                "gpt-5",
                false
        );

        ChatClient chatClient = provider.chatClient();

        assertNotNull(chatClient);
    }

    @Test
    void chatClient_rejectsMissingPreferenceWithoutBlockingApplicationStartup() {
        PreferenceService preferenceService = mock(PreferenceService.class);
        when(preferenceService.loadPreferences())
                .thenReturn(new UserPreference(null, null, null));
        OpenAiAiProvider provider = new OpenAiAiProvider(
                preferenceService,
                new ObjectMapper(),
                "https://api.openai.com",
                "gpt-5",
                false
        );

        assertThrows(IllegalStateException.class, provider::chatClient);
    }

    @Test
    void chatClient_reusesInstanceForSameKeyAndRebuildsWhenKeyChanges() {
        PreferenceService preferenceService = mock(PreferenceService.class);
        when(preferenceService.loadPreferences())
                .thenReturn(new UserPreference(null, null, "openai-key-1"))
                .thenReturn(new UserPreference(null, null, "openai-key-1"))
                .thenReturn(new UserPreference(null, null, "openai-key-2"));
        OpenAiAiProvider provider = new OpenAiAiProvider(
                preferenceService,
                new ObjectMapper(),
                "https://api.openai.com",
                "gpt-5",
                false
        );

        ChatClient first = provider.chatClient();
        ChatClient second = provider.chatClient();
        ChatClient third = provider.chatClient();

        assertSame(first, second);
        assertNotSame(second, third);
    }
}
