package alma.llm;

import alma.config.AlmaConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

public final class OpenAiClient {

    private final ChatLanguageModel probeModel;
    private final ChatLanguageModel execModel;
    private final String probeModelName;
    private final String execModelName;

    public OpenAiClient(AlmaConfig config) {
        this.probeModelName = config.modelProbe();
        this.execModelName  = config.modelExec();
        this.probeModel = build(config, probeModelName);
        this.execModel  = build(config, execModelName);
    }

    public String chatProbe(String systemPrompt, String userPrompt) {
        return chat(probeModel, probeModelName, systemPrompt, userPrompt);
    }

    public String chatExec(String systemPrompt, String userPrompt) {
        return chat(execModel, execModelName, systemPrompt, userPrompt);
    }

    private static String chat(ChatLanguageModel model, String modelName,
                               String systemPrompt, String userPrompt) {
        Response<AiMessage> response = model.generate(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
        );
        TokenUsageLogger.record(modelName, response.tokenUsage());
        return response.content().text();
    }

    private static ChatLanguageModel build(AlmaConfig config, String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(config.openAiApiKey())
                .modelName(modelName)
                .temperature(0.2)
                .maxRetries(1)
                .build();
    }
}
