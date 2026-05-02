package alma.core.pipeline;

import alma.core.AgentContext;
import alma.core.Phase;

public final class IntakePhase implements Phase {

    @Override
    public String name() {
        return "Intake";
    }

    @Override
    public void run(AgentContext context) {
        // StemCell already wrote rawInput. No LLM call here.
    }
}
