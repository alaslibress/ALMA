package alma.agents;

import alma.llm.OpenAiClient;
import alma.llm.ToolRegistry;

// Single class. Behavior fully driven by the Blueprint at construction.
public class SpecializedAgent {

    private final Blueprint blueprint;
    private final OpenAiClient client;
    private final ToolRegistry registry;

    public SpecializedAgent(Blueprint blueprint, OpenAiClient client, ToolRegistry registry) {
        this.blueprint = blueprint;
        this.client = client;
        this.registry = registry;
    }

    public String execute(String input) {
        // Tool wiring comes in Sub-Plan 05. For now plain chat.
        return client.chatExec(blueprint.systemPrompt(), input);
    }

    public String name() {
        return blueprint.title();
    }

    public Blueprint blueprint() {
        return blueprint;
    }

    protected OpenAiClient client() {
        return client;
    }

    protected ToolRegistry registry() {
        return registry;
    }
}
