package alma.core.pipeline;

import alma.agents.Blueprint;
import alma.agents.EvalReport;
import alma.agents.EvalTask;
import alma.agents.EvalTaskSeed;
import alma.agents.ProbeReport;
import alma.agents.SpecializedAgent;
import alma.agents.TaskScore;
import alma.core.AgentContext;
import alma.core.MutationException;
import alma.core.Phase;
import alma.eval.Scorer;

import java.util.ArrayList;
import java.util.List;

// Single-use phase. StemCell creates a fresh instance per run.
public final class SafeguardLoopPhase implements Phase {

    private static final int PATIENCE = 2;
    private static final double FAILURE_THRESHOLD = 0.5;

    private final AgentFactory factory;
    private final BlueprintRefiner refiner;
    private final Scorer scorer;

    private AgentContext context;
    private Blueprint current;
    private SpecializedAgent currentAgent;
    private EvalReport currentReport;
    private List<EvalTask> tests;
    private int roundsWithoutImprovement;

    public SafeguardLoopPhase(AgentFactory factory, BlueprintRefiner refiner, Scorer scorer) {
        this.factory = factory;
        this.refiner = refiner;
        this.scorer = scorer;
    }

    @Override
    public String name() {
        return "SafeguardLoop";
    }

    @Override
    public void run(AgentContext incoming) throws MutationException {
        if (!setup(incoming)) {
            return;
        }
        if (atThreshold(0)) {
            return;
        }

        for (int round = 1; round <= current.maxRefinementRounds(); round++) {
            if (!step(round)) {
                return;
            }
        }
        context.appendTrace("stop: budget exhausted at round " + current.maxRefinementRounds());
    }

    private boolean setup(AgentContext incoming) throws MutationException {
        this.context = incoming;
        this.current = incoming.blueprint();
        this.currentAgent = incoming.agent();
        if (current == null || currentAgent == null) {
            throw new MutationException("SafeguardLoop requires a Blueprint and an Agent.");
        }

        this.tests = synthesizeTests(incoming.probe());
        if (tests.isEmpty()) {
            context.appendTrace("round 0: no synthetic tasks; skipping loop.");
            return false;
        }

        this.currentReport = score(currentAgent, tests);
        context.appendTrace(traceLine(0, currentReport.average(), "initial"));
        this.roundsWithoutImprovement = 0;
        return true;
    }

    private boolean atThreshold(int round) {
        if (currentReport.average() >= current.scoreThreshold()) {
            context.appendTrace("stop: threshold met at round " + round);
            return true;
        }
        return false;
    }

    private boolean step(int round) {
        Blueprint candidate;
        try {
            candidate = refiner.refine(current, currentReport);
        } catch (MutationException refineError) {
            context.appendTrace("stop: refinement error round " + round + ": " + refineError.getMessage());
            rollbackIfPossible();
            return false;
        }

        SpecializedAgent candidateAgent;
        EvalReport candidateReport;
        try {
            candidateAgent = factory.build(candidate);
            candidateReport = score(candidateAgent, tests);
        } catch (Exception buildError) {
            context.appendTrace("stop: mutation error round " + round + ": " + buildError.getMessage());
            rollbackIfPossible();
            return false;
        }

        promoteIfBetter(round, candidate, candidateAgent, candidateReport);

        if (atThreshold(round)) {
            return false;
        }
        if (roundsWithoutImprovement >= PATIENCE) {
            context.appendTrace("stop: " + PATIENCE + " rounds without improvement at round " + round);
            return false;
        }
        return true;
    }

    private void promoteIfBetter(int round, Blueprint candidate, SpecializedAgent candidateAgent, EvalReport candidateReport) {
        Decision decision = decide(currentReport.average(), candidateReport.average());
        if (decision.promote()) {
            context.promote(candidate, candidateAgent, current, currentAgent);
            current = candidate;
            currentAgent = candidateAgent;
            currentReport = candidateReport;
        }
        if (decision.resetsPatience()) {
            roundsWithoutImprovement = 0;
        } else {
            roundsWithoutImprovement++;
        }
        context.appendTrace(traceLine(round, candidateReport.average(), decision.tag()));
    }

    // Tie-break: newer wins, plateau still counts.
    private static Decision decide(double currentScore, double candidateScore) {
        if (candidateScore > currentScore)  return new Decision(true,  true,  "promoted");
        if (candidateScore == currentScore) return new Decision(true,  false, "tied (newer wins, plateau counted)");
        return new Decision(false, false, "rejected");
    }

    private record Decision(boolean promote, boolean resetsPatience, String tag) {}

    private EvalReport score(SpecializedAgent agent, List<EvalTask> tasks) {
        List<TaskScore> scores = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        double total = 0;
        for (EvalTask task : tasks) {
            String hyp = agent.execute(task.input());
            double s = scorer.score(hyp, task);
            scores.add(new TaskScore(task.id(), s, hyp));
            total += s;
            if (s < FAILURE_THRESHOLD) {
                failures.add(task.id() + ": score=" + s + " hyp=" + truncate(hyp, 80));
            }
        }
        double average = tasks.isEmpty() ? 0.0 : total / tasks.size();
        return new EvalReport(scores, average, failures);
    }

    private static List<EvalTask> synthesizeTests(ProbeReport probe) {
        if (probe == null || probe.syntheticTasks() == null) {
            return List.of();
        }
        List<EvalTask> tasks = new ArrayList<>();
        int index = 0;
        for (EvalTaskSeed seed : probe.syntheticTasks()) {
            String reference = seed.expectedReference() == null ? "" : seed.expectedReference();
            tasks.add(new EvalTask("synthetic-" + (index++), seed.input(), List.of(reference)));
        }
        return tasks;
    }

    private void rollbackIfPossible() {
        Blueprint lastBlueprint = context.lastGoodBlueprint();
        SpecializedAgent lastAgent = context.lastGoodAgent();
        context.rollback();
        if (lastBlueprint != null) {
            current = lastBlueprint;
            currentAgent = lastAgent;
        }
    }

    private static String traceLine(int round, double average, String tag) {
        return String.format("round %d: score=%.3f %s", round, average, tag);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
