package ch.andreskonrad.torenta.concierge.service.provider;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import ch.andreskonrad.torenta.concierge.dto.CandidateRanking;
import ch.andreskonrad.torenta.concierge.dto.CandidateRankings;
import ch.andreskonrad.torenta.concierge.dto.MediaCandidate;
import ch.andreskonrad.torenta.concierge.dto.SearchIntent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractChatClientAiProviderTest {

    @Test
    void extractsIntentAndReturnsStructuredRankings() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        SearchIntent intent = intent();
        CandidateRanking ranking = new CandidateRanking("MOVIE:1", 90, "Strong match");
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(SearchIntent.class)).thenReturn(intent);
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(CandidateRankings.class))
                .thenReturn(new CandidateRankings(List.of(ranking)));
        TestProvider provider = new TestProvider(new ObjectMapper(), true, chatClient);

        assertSame(intent, provider.extractIntent("science fiction"));
        assertEquals(List.of(ranking), provider.rank(
                "science fiction", intent, List.of(candidate())
        ));
    }

    @Test
    void convertsMissingRankingPayloadsToEmptyLists() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .entity(CandidateRankings.class))
                .thenReturn(null)
                .thenReturn(new CandidateRankings(null));
        TestProvider provider = new TestProvider(new ObjectMapper(), false, chatClient);

        assertEquals(List.of(), provider.rank("request", intent(), List.of(candidate())));
        assertEquals(List.of(), provider.rank("request", intent(), List.of(candidate())));
    }

    @Test
    void reportsRankingInputSerializationFailure() throws JacksonException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JacksonException("serialization failed") {
                });
        TestProvider provider = new TestProvider(objectMapper, false, chatClient);

        assertThrows(IllegalStateException.class, () ->
                provider.rank("request", intent(), List.of(candidate())));
    }

    private SearchIntent intent() {
        return new SearchIntent(
                AiMediaType.MOVIE, List.of(), null, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }

    private MediaCandidate candidate() {
        return new MediaCandidate(
                "MOVIE:1", AiMediaType.MOVIE, 1, "Movie", "Overview", List.of(),
                "2020-01-01", "en", 10, 8, 100, null, null
        );
    }

    private static final class TestProvider extends AbstractChatClientAiProvider {

        private final ChatClient chatClient;

        private TestProvider(
                ObjectMapper objectMapper,
                boolean interactionLoggingEnabled,
                ChatClient chatClient
        ) {
            super(objectMapper, interactionLoggingEnabled);
            this.chatClient = chatClient;
        }

        @Override
        public AiProviderType type() {
            return AiProviderType.OLLAMA;
        }

        @Override
        protected ChatClient chatClient() {
            return chatClient;
        }
    }
}
