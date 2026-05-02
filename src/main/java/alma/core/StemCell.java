package alma.core;

import alma.agents.SpecializedAgent;
import alma.cli.ui.EnglishOnlyLanguageProvider;
import alma.cli.ui.GhostStateTracker;
import alma.cli.ui.GhostStates;
import alma.cli.ui.LanguageBadge;
import alma.cli.ui.LanguageProvider;
import alma.cli.ui.PhaseRenderer;
import alma.config.AlmaConfig;
import alma.core.pipeline.AgentFactory;
import alma.core.pipeline.BlueprintPhase;
import alma.core.pipeline.BlueprintRefiner;
import alma.core.pipeline.ExecutionPhase;
import alma.core.pipeline.IntakePhase;
import alma.core.pipeline.LlmBlueprintRefiner;
import alma.core.pipeline.MutationPhase;
import alma.core.pipeline.ProbePhase;
import alma.core.pipeline.ReportPhase;
import alma.core.pipeline.SafeguardLoopPhase;
import alma.eval.ChrfScorer;
import alma.eval.CompositeScorer;
import alma.eval.JudgeScorer;
import alma.eval.Scorer;
import alma.llm.DefaultToolRegistry;
import alma.llm.OpenAiClient;
import alma.llm.ToolRegistry;

import java.io.PrintStream;
import java.util.List;

public final class StemCell {

    private final List<Phase> phases;
    private final ToolRegistry registry;
    private final String targetLanguage;

    public StemCell(AlmaConfig config, String targetLanguage) {
        OpenAiClient client = new OpenAiClient(config);
        ToolRegistry tools = new DefaultToolRegistry();
        Scorer scorer = new CompositeScorer(new ChrfScorer(), new JudgeScorer(client));
        BlueprintRefiner refiner = new LlmBlueprintRefiner(client);
        AgentFactory factory = blueprint -> {
            tools.resolve(blueprint.toolNames());
            return new SpecializedAgent(blueprint, client, tools);
        };

        this.registry = tools;
        this.targetLanguage = targetLanguage;
        this.phases = List.of(
                new IntakePhase(),
                new ProbePhase(client),
                new BlueprintPhase(client),
                new MutationPhase(factory),
                new SafeguardLoopPhase(factory, refiner, scorer),
                new ExecutionPhase(),
                new ReportPhase()
        );
    }

    public int run(String rawInput) {
        AgentContext context = new AgentContext();
        context.setRawInput(rawInput);

        PrintStream out = System.out;
        PhaseRenderer renderer = new PhaseRenderer(out);
        GhostStateTracker ghost = new GhostStateTracker(out);
        ghost.markInitial(GhostStates.BASE);
        LanguageBadge badge = new LanguageBadge(providerFor(registry));

        int total = phases.size();
        for (int index = 0; index < total; index++) {
            Phase phase = phases.get(index);
            ghost.show(ghostFor(phase.name()));
            renderer.phaseStart(index + 1, total, phase.name());
            try {
                phase.run(context);
                renderer.phaseOk();
                if (phase.name().equals("Intake")) {
                    badge.print(out, context.rawInput(), targetLanguage);
                }
            } catch (MutationException error) {
                renderer.phaseFail(error.getMessage());
                ghost.show(GhostStates.ERROR);
                return 1;
            }
        }
        return 0;
    }

    private static GhostStates ghostFor(String phaseName) {
        return switch (phaseName) {
            case "Intake"        -> GhostStates.THINKING;
            case "Probe"         -> GhostStates.PROBING;
            case "Blueprint"     -> GhostStates.PROBING;
            case "Mutation"      -> GhostStates.MUTATING;
            case "SafeguardLoop" -> GhostStates.MUTATING;
            case "Execution"     -> GhostStates.SPEAKING;
            case "Report"        -> GhostStates.SPEAKING;
            default              -> GhostStates.BASE;
        };
    }

    private static LanguageProvider providerFor(ToolRegistry registry) {
        if (registry instanceof DefaultToolRegistry def) {
            return def.languageDetector();
        }
        return new EnglishOnlyLanguageProvider();
    }
}
