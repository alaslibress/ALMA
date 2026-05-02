package alma.core.pipeline;

import alma.agents.Blueprint;
import alma.agents.SpecializedAgent;

@FunctionalInterface
public interface AgentFactory {

    SpecializedAgent build(Blueprint blueprint);
}
