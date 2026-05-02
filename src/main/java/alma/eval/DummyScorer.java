package alma.eval;

import alma.agents.EvalTask;

// Constant value. Day-3 wiring before real metrics land in Sub-Plan 07.
public final class DummyScorer implements Scorer {

    private static final double CONSTANT = 0.7;

    @Override
    public double score(String hypothesis, EvalTask task) {
        return CONSTANT;
    }
}
