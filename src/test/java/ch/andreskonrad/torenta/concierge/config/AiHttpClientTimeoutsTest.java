package ch.andreskonrad.torenta.concierge.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiHttpClientTimeoutsTest {

    @Test
    void configuresOllamaRequestTimeout() {
        Duration timeout = Duration.ofSeconds(12);

        Object configuredTimeout = ReflectionTestUtils.getField(
                AiHttpClientTimeouts.ollamaRequestFactory(timeout),
                "readTimeout"
        );

        assertEquals(timeout, configuredTimeout);
    }

    @Test
    void configuresOpenAiRequestTimeout() {
        Duration timeout = Duration.ofSeconds(12);
        SpringAiOpenAiHttpClient.Builder builder = mock(SpringAiOpenAiHttpClient.Builder.class);

        AiHttpClientTimeouts.openAiHttpClientBuilderCustomizer(timeout).customize(builder);

        verify(builder).timeout(timeout);
    }
}
