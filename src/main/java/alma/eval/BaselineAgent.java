package alma.eval;

import alma.llm.OpenAiClient;

// Single LLM call. No probe, no blueprint, no tools, no refinement. The "before".
public final class BaselineAgent {

    private static final String SYSTEM_PROMPT =
            "You are a helpful translator, transcreator, and style editor for English, "
            + "Spanish, and Portuguese. Read the user's task carefully and return only "
            + "the requested output, no explanations.";

    private final OpenAiClient client;

    public BaselineAgent(OpenAiClient client) {
        this.client = client;
    }

    public String execute(String input) {
        return client.chatExec(SYSTEM_PROMPT, input);
    }
}
