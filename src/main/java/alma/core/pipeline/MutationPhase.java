package alma.core.pipeline;

import alma.agents.Blueprint;
import alma.agents.SpecializedAgent;
import alma.core.AgentContext;
import alma.core.MutationException;
import alma.core.Phase;

public final class MutationPhase implements Phase {

    private final AgentFactory factory;

    public MutationPhase(AgentFactory factory) {
        this.factory = factory;
    }

    @Override
    public String name() {
        return "Mutation";
    }

    @Override
    public void run(AgentContext context) throws MutationException {
        Blueprint blueprint = context.blueprint();
        if (blueprint == null) {
            throw new MutationException("Mutation requires a Blueprint.");
        }
        try {
            SpecializedAgent agent = factory.build(blueprint);
            context.setAgent(agent);
        } catch (Exception buildError) {
            throw new MutationException("Mutation failed: " + buildError.getMessage(), buildError);
        }
    }
}
