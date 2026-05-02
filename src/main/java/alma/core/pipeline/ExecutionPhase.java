package alma.core.pipeline;

import alma.agents.SpecializedAgent;
import alma.core.AgentContext;
import alma.core.MutationException;
import alma.core.Phase;

public final class ExecutionPhase implements Phase {

    @Override
    public String name() {
        return "Execution";
    }

    @Override
    public void run(AgentContext context) throws MutationException {
        SpecializedAgent agent = context.agent();
        if (agent == null) {
            throw new MutationException("Execution requires a SpecializedAgent.");
        }
        try {
            String result = agent.execute(context.rawInput());
            context.setResult(result);
        } catch (Exception apiError) {
            throw new MutationException("Execution failed: " + apiError.getMessage(), apiError);
        }
    }
}
