package ch.andreskonrad.torenta.concierge.config;

import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

public final class AiHttpClientTimeouts {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private AiHttpClientTimeouts() {
    }

    public static RestClient.Builder ollamaRestClientBuilder() {
        return RestClient.builder().requestFactory(ollamaRequestFactory(REQUEST_TIMEOUT));
    }

    static JdkClientHttpRequestFactory ollamaRequestFactory(Duration timeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }

    public static OpenAiHttpClientBuilderCustomizer openAiHttpClientBuilderCustomizer() {
        return openAiHttpClientBuilderCustomizer(REQUEST_TIMEOUT);
    }

    static OpenAiHttpClientBuilderCustomizer openAiHttpClientBuilderCustomizer(Duration timeout) {
        return builder -> builder.timeout(timeout);
    }
}
