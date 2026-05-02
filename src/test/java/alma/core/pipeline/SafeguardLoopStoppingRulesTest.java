package alma.core.pipeline;

import alma.agents.Blueprint;
import alma.agents.EvalTask;
import alma.agents.EvalTaskSeed;
import alma.agents.ProbeReport;
import alma.agents.SpecializedAgent;
import alma.core.AgentContext;
import alma.core.MutationException;
import alma.eval.Scorer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

final class SafeguardLoopStoppingRulesTest {

    @Test
    void stopsAtThresholdRound0() throws Exception {
        Blueprint blueprint = blueprint(0.5, 5);
        AgentContext context = contextWith(blueprint);

        Scorer perfect = (hyp, task) -> 0.9;
        AgentFactory factory = bp -> stubAgent(bp, "out");
        BlueprintRefiner refiner = (cur, rep) -> {
            throw new AssertionError("Refiner should not be called when threshold met at round 0.");
        };

        new SafeguardLoopPhase(factory, refiner, perfect).run(context);

        String trace = String.join(" | ", context.evolutionTrace());
        assertThat(trace).contains("threshold met at round 0");
    }

    @Test
    void stopsAfterTwoRoundsWithoutImprovement() throws Exception {
        Blueprint blueprint = blueprint(0.99, 5);
        AgentContext context = contextWith(blueprint);

        Scorer constant = (hyp, task) -> 0.5;
        AgentFactory factory = bp -> stubAgent(bp, "out");
        BlueprintRefiner refiner = (cur, rep) -> cur;

        new SafeguardLoopPhase(factory, refiner, constant).run(context);

        String trace = String.join(" | ", context.evolutionTrace());
        assertThat(trace).contains("rounds without improvement at round 2");
    }

    @Test
    void stopsAtBudgetExhausted() throws Exception {
        Blueprint blueprint = blueprint(0.99, 2);
        AgentContext context = contextWith(blueprint);

        // Strictly increasing scorer keeps "no-improvement" counter at zero.
        double[] series = { 0.30, 0.40, 0.50, 0.60, 0.70 };
        int[] callCount = { 0 };
        Scorer rising = (hyp, task) -> {
            int idx = Math.min(callCount[0]++, series.length - 1);
            return series[idx];
        };
        AgentFactory factory = bp -> stubAgent(bp, "out");
        BlueprintRefiner refiner = (cur, rep) -> cur;

        new SafeguardLoopPhase(factory, refiner, rising).run(context);

        String trace = String.join(" | ", context.evolutionTrace());
        assertThat(trace).contains("budget exhausted");
    }

    @Test
    void rollsBackOnMutationError() throws Exception {
        Blueprint blueprint = blueprint(0.99, 3);
        AgentContext context = contextWith(blueprint);

        Scorer constant = (hyp, task) -> 0.5;
        AgentFactory factory = bp -> {
            throw new IllegalStateException("simulated mutation failure");
        };
        BlueprintRefiner refiner = (cur, rep) -> cur;

        new SafeguardLoopPhase(factory, refiner, constant).run(context);

        String trace = String.join(" | ", context.evolutionTrace());
        assertThat(trace).contains("mutation error");
    }

    @Test
    void rollbackRestoresBlueprintAndAgentTogether() throws Exception {
        Blueprint base = blueprint(0.99, 3);
        Blueprint better = blueprint(0.99, 3);
        AgentContext context = contextWith(base);

        SpecializedAgent baseAgent = stubAgent(base, "base-out");
        SpecializedAgent betterAgent = stubAgent(better, "better-out");
        context.setAgent(baseAgent);

        int[] call = { 0 };
        AgentFactory factory = bp -> {
            if (call[0]++ == 0) return betterAgent;
            throw new IllegalStateException("simulated mutation failure on round 2");
        };
        BlueprintRefiner refiner = (cur, rep) -> cur == base ? better : cur;
        Scorer scorer = (hyp, task) -> hyp.equals("base-out") ? 0.4 : 0.6;

        new SafeguardLoopPhase(factory, refiner, scorer).run(context);

        assertThat(context.blueprint()).isSameAs(base);
        assertThat(context.agent()).isSameAs(baseAgent);
        assertThat(context.lastGoodBlueprint()).isSameAs(base);
        assertThat(context.lastGoodAgent()).isSameAs(baseAgent);
    }

    @Test
    void exactTiePromotesNewButCountsPlateau() throws Exception {
        Blueprint a = blueprint(0.99, 5);
        Blueprint b = blueprint(0.99, 5);
        AgentContext context = contextWith(a);

        SpecializedAgent agentA = stubAgent(a, "A");
        SpecializedAgent agentB = stubAgent(b, "B");
        context.setAgent(agentA);

        AgentFactory factory = bp -> agentB;
        BlueprintRefiner refiner = (cur, rep) -> b;
        Scorer constant = (hyp, task) -> 0.5;

        new SafeguardLoopPhase(factory, refiner, constant).run(context);

        String trace = String.join(" | ", context.evolutionTrace());
        assertThat(trace).contains("tied");
        assertThat(trace).contains("rounds without improvement at round 2");
        assertThat(context.blueprint()).isSameAs(b);
    }

    @Test
    void requiresBlueprintAndAgent() {
        AgentContext context = new AgentContext();
        context.setRawInput("hi");

        Scorer scorer = (hyp, task) -> 0.5;
        AgentFactory factory = bp -> stubAgent(bp, "out");
        BlueprintRefiner refiner = (cur, rep) -> cur;

        Throwable error = catchThrowable(() ->
                new SafeguardLoopPhase(factory, refiner, scorer).run(context));

        assertThat(error).isInstanceOf(MutationException.class);
    }

    // --- helpers ---

    private static Blueprint blueprint(double threshold, int maxRounds) {
        return new Blueprint(
                "T",
                "system",
                List.of(),
                List.of(),
                "gpt-4o",
                threshold,
                maxRounds
        );
    }

    private static AgentContext contextWith(Blueprint blueprint) {
        AgentContext context = new AgentContext();
        context.setRawInput("input");
        context.setProbe(new ProbeReport(
                List.of("translation"),
                List.of(),
                List.of(),
                List.of(new EvalTaskSeed("hello", "hola"),
                        new EvalTaskSeed("world", "mundo")),
                "test"
        ));
        context.setBlueprint(blueprint);
        context.setAgent(stubAgent(blueprint, "stub"));
        return context;
    }

    private static SpecializedAgent stubAgent(Blueprint blueprint, String fixedOutput) {
        return new SpecializedAgent(blueprint, null, null) {
            @Override
            public String execute(String input) {
                return fixedOutput;
            }
        };
    }
}
