package ch.andreskonrad.torenta.concierge.service.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AiProviderFactory {

    private final AiProviderType selectedType;
    private final Map<AiProviderType, AiProvider> providers;

    @Autowired
    public AiProviderFactory(
            @Value("${app.ai.provider:OLLAMA}") String selectedProvider,
            List<AiProvider> providers
    ) {
        this.selectedType = AiProviderType.valueOf(selectedProvider.trim().toUpperCase(Locale.ROOT));
        this.providers = new EnumMap<>(AiProviderType.class);
        for (AiProvider provider : providers) {
            if (this.providers.put(provider.type(), provider) != null) {
                throw new IllegalStateException("Multiple AI providers registered for " + provider.type());
            }
        }
    }

    public AiProvider selectedProvider() {
        AiProvider provider = providers.get(selectedType);
        if (provider == null) {
            throw new IllegalStateException("AI provider is not available: " + selectedType);
        }
        return provider;
    }
}
