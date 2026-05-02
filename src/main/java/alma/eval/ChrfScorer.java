package alma.eval;

import alma.agents.EvalTask;

public final class ChrfScorer implements Scorer {

    @Override
    public double score(String hypothesis, EvalTask task) {
        return Metric.chrF(hypothesis, task.references());
    }
}
