package ch.andreskonrad.torenta.concierge.service.provider;

import ch.andreskonrad.torenta.preference.service.PreferenceService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static ch.andreskonrad.torenta.concierge.config.AiHttpClientTimeouts.openAiHttpClientBuilderCustomizer;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "OPENAI")
public class OpenAiAiProvider extends AbstractChatClientAiProvider {

    private final PreferenceService preferenceService;
    private final String baseUrl;
    private final String model;
    private final Object chatClientLock = new Object();
    private volatile String cachedApiKey;
    private volatile ChatClient cachedChatClient;

    public OpenAiAiProvider(
            PreferenceService preferenceService,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.openai.model:gpt-5}") String model,
            @Value("${app.ai.logging.enabled:false}") boolean interactionLoggingEnabled
    ) {
        super(objectMapper, interactionLoggingEnabled);
        this.preferenceService = preferenceService;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    protected ChatClient chatClient() {
        String apiKey = preferenceService.loadPreferences().getOpenAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured in user preferences");
        }
        ChatClient existingChatClient = cachedChatClient;
        if (existingChatClient != null && apiKey.equals(cachedApiKey)) {
            return existingChatClient;
        }

        synchronized (chatClientLock) {
            if (cachedChatClient != null && apiKey.equals(cachedApiKey)) {
                return cachedChatClient;
            }

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .model(model)
                    .build();
            ChatClient rebuiltChatClient = ChatClient.create(OpenAiChatModel.builder()
                    .options(options)
                    .httpClientBuilderCustomizer(openAiHttpClientBuilderCustomizer())
                    .build());
            cachedApiKey = apiKey;
            cachedChatClient = rebuiltChatClient;
            return rebuiltChatClient;
        }
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.OPENAI;
    }
}
