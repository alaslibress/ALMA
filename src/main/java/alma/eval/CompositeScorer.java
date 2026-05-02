package alma.eval;

import alma.agents.EvalTask;

// Routes to chrF when references are present, judge otherwise.
public final class CompositeScorer implements Scorer {

    private final Scorer chrf;
    private final Scorer judge;

    public CompositeScorer(Scorer chrf, Scorer judge) {
        this.chrf = chrf;
        this.judge = judge;
    }

    @Override
    public double score(String hypothesis, EvalTask task) {
        if (task.references() != null && !task.references().isEmpty()) {
            return chrf.score(hypothesis, task);
        }
        return judge.score(hypothesis, task);
    }
}
