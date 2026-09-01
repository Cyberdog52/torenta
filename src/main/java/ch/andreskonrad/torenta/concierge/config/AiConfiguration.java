package ch.andreskonrad.torenta.concierge.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "OLLAMA", matchIfMissing = true)
    ChatModel ollamaChatModel(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.model:qwen3:8b}") String model
    ) {
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .disableThinking()
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder()
                        .baseUrl(baseUrl)
                        .restClientBuilder(AiHttpClientTimeouts.ollamaRestClientBuilder())
                        .build())
                .options(options)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "OLLAMA", matchIfMissing = true)
    ChatClient aiChatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
