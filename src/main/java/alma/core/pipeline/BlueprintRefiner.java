package alma.core.pipeline;

import alma.agents.Blueprint;
import alma.agents.EvalReport;
import alma.core.MutationException;

@FunctionalInterface
public interface BlueprintRefiner {

    Blueprint refine(Blueprint current, EvalReport report) throws MutationException;
}
