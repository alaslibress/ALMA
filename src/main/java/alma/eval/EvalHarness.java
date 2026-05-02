package alma.eval;

import alma.agents.EvalTask;
import alma.agents.SpecializedAgent;
import alma.config.AlmaConfig;
import alma.core.AgentContext;
import alma.core.MutationException;
import alma.core.pipeline.AgentFactory;
import alma.core.pipeline.BlueprintPhase;
import alma.core.pipeline.BlueprintRefiner;
import alma.core.pipeline.ExecutionPhase;
import alma.core.pipeline.IntakePhase;
import alma.core.pipeline.LlmBlueprintRefiner;
import alma.core.pipeline.MutationPhase;
import alma.core.pipeline.ProbePhase;
import alma.core.pipeline.SafeguardLoopPhase;
import alma.llm.DefaultToolRegistry;
import alma.llm.OpenAiClient;
import alma.llm.ToolRegistry;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EvalHarness {

    private static final String BENCHMARK_RESOURCE = "benchmarks/linguistic.json";
    static final String RUN_FAILED_MARKER = "<<RUN-FAILED>>";

    private final OpenAiClient client;
    private final ToolRegistry registry;
    private final BaselineAgent baseline;
    private final Scorer chrfScorer;
    private final Scorer judgeScorer;
    private final Scorer loopScorer;
    private final BlueprintRefiner refiner;

    public EvalHarness(AlmaConfig config) {
        this.client = new OpenAiClient(config);
        this.registry = new DefaultToolRegistry();
        this.baseline = new BaselineAgent(client);
        this.chrfScorer = new ChrfScorer();
        this.judgeScorer = new JudgeScorer(client);
        this.loopScorer = new CompositeScorer(chrfScorer, judgeScorer);
        this.refiner = new LlmBlueprintRefiner(client);
    }

    public int run(PrintStream out) {
        List<BenchmarkEntry> entries = BenchmarkSet.load(BENCHMARK_RESOURCE);
        if (entries.isEmpty()) {
            out.println("[evaluate] benchmark is empty.");
            return 0;
        }

        out.println("Running " + entries.size() + " benchmark entries...");
        List<EvalRow> rows = new ArrayList<>();
        int index = 1;
        for (BenchmarkEntry entry : entries) {
            EvalRow row = evaluateOne(entry);
            rows.add(row);
            out.printf(Locale.ROOT,
                    "  %2d/%d %-22s baseline=%.3f[%s] alma=%.3f[%s] delta=%+.3f%n",
                    index++, entries.size(), entry.id(),
                    row.baselineScore(), row.baselineStatus(),
                    row.almaScore(), row.almaStatus(),
                    row.delta());
        }

        printSummaryTable(out, rows);
        Path report = EvalReportWriter.writeMarkdown(rows);
        out.println();
        out.println("Report: " + report.toAbsolutePath());
        return 0;
    }

    private EvalRow evaluateOne(BenchmarkEntry entry) {
        RunOutcome baselineRun = safeRun(entry.id(), "baseline",
                () -> baseline.execute(entry.input()));
        AlmaRun almaRun = runAlmaCaptured(entry.id(), entry.input());

        double bScore = baselineRun.status() == RunStatus.OK
                ? scoreFor(entry, baselineRun.output()) : 0.0;
        double aScore = almaRun.status() == RunStatus.OK
                ? scoreFor(entry, almaRun.output()) : 0.0;

        return new EvalRow(entry, bScore, aScore,
                baselineRun.output(), almaRun.output(),
                baselineRun.status(), almaRun.status(),
                almaRun.trace());
    }

    private AlmaRun runAlmaCaptured(String entryId, String input) {
        AgentContext context = new AgentContext();
        context.setRawInput(input);

        AgentFactory factory = blueprint -> {
            registry.resolve(blueprint.toolNames());
            return new SpecializedAgent(blueprint, client, registry);
        };

        try {
            new IntakePhase().run(context);
            new ProbePhase(client).run(context);
            new BlueprintPhase(client).run(context);
            new MutationPhase(factory).run(context);
            new SafeguardLoopPhase(factory, refiner, loopScorer).run(context);
            new ExecutionPhase().run(context);
        } catch (Exception error) {
            System.err.println("[evaluate] " + entryId + " alma failed: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
            return new AlmaRun(RUN_FAILED_MARKER, RunStatus.FAILED,
                    List.copyOf(context.evolutionTrace()));
        }

        String output = context.result() == null ? "" : context.result();
        RunStatus status = output.isBlank() ? RunStatus.EMPTY : RunStatus.OK;
        return new AlmaRun(output.isBlank() ? "" : output, status,
                List.copyOf(context.evolutionTrace()));
    }

    private record AlmaRun(String output, RunStatus status, List<String> trace) {}

    private double scoreFor(BenchmarkEntry entry, String hypothesis) {
        EvalTask task = new EvalTask(entry.id(), entry.input(), entry.references());
        return useChrf(entry) ? chrfScorer.score(hypothesis, task) : judgeScorer.score(hypothesis, task);
    }

    private static boolean useChrf(BenchmarkEntry entry) {
        String type = entry.subType();
        boolean hasReferences = entry.references() != null && !entry.references().isEmpty();
        if (type == null) return hasReferences;
        if (type.equals("translation")) return true;
        if (type.equals("localization") && hasReferences) return true;
        return false;
    }

    private static void printSummaryTable(PrintStream out, List<EvalRow> rows) {
        Map<String, double[]> bySubType = EvalReportWriter.aggregate(rows);
        out.println();
        out.println("metric            | baseline |  alma  | delta");
        out.println("------------------|----------|--------|-------");
        for (Map.Entry<String, double[]> entry : bySubType.entrySet()) {
            double[] sums = entry.getValue();
            double bAvg = sums[2] == 0 ? 0 : sums[0] / sums[2];
            double aAvg = sums[2] == 0 ? 0 : sums[1] / sums[2];
            out.printf(Locale.ROOT, "%-17s |  %5.3f   | %5.3f  | %+.3f%n",
                    entry.getKey(), bAvg, aAvg, aAvg - bAvg);
        }
    }

    private static RunOutcome safeRun(String entryId, String side,
                                      ThrowingSupplier<String> supplier) {
        try {
            String result = supplier.get();
            if (result == null || result.isBlank()) {
                return new RunOutcome("", RunStatus.EMPTY);
            }
            return new RunOutcome(result, RunStatus.OK);
        } catch (Exception error) {
            System.err.println("[evaluate] " + entryId + " " + side + " failed: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
            return new RunOutcome(RUN_FAILED_MARKER, RunStatus.FAILED);
        }
    }

    private record RunOutcome(String output, RunStatus status) {}

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
