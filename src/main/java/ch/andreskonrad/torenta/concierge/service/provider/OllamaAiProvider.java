package ch.andreskonrad.torenta.concierge.service.provider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "OLLAMA", matchIfMissing = true)
public class OllamaAiProvider extends AbstractChatClientAiProvider {

    private final ChatClient chatClient;

    public OllamaAiProvider(
            ChatClient chatClient,
            ObjectMapper objectMapper,
            @Value("${app.ai.logging.enabled:false}") boolean interactionLoggingEnabled
    ) {
        super(objectMapper, interactionLoggingEnabled);
        this.chatClient = chatClient;
    }

    @Override
    protected ChatClient chatClient() {
        return chatClient;
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.OLLAMA;
    }
}
