package alma.eval;

import alma.agents.EvalTask;

@FunctionalInterface
public interface Scorer {

    double score(String hypothesis, EvalTask task);
}
