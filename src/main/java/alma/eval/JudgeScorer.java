package alma.eval;

import alma.agents.EvalTask;
import alma.llm.AlmaJackson;
import alma.llm.JsonExtractor;
import alma.llm.OpenAiClient;
import alma.llm.PromptTemplates;

import java.util.HashMap;
import java.util.Map;

public final class JudgeScorer implements Scorer {

    private static final double NEUTRAL_FALLBACK = 0.5;

    private final OpenAiClient client;

    public JudgeScorer(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public double score(String hypothesis, EvalTask task) {
        try {
            JudgeReport report = judge(hypothesis, task);
            return clamp01(report.overall() / 10.0);
        } catch (Exception error) {
            return NEUTRAL_FALLBACK;
        }
    }

    private JudgeReport judge(String hypothesis, EvalTask task) throws Exception {
        Map<String, String> vars = new HashMap<>();
        vars.put("task_input",     orEmpty(task.input()));
        vars.put("task_reference", task.references() == null ? "" : String.join(" / ", task.references()));
        vars.put("task_notes",     "");
        vars.put("hypothesis",     orEmpty(hypothesis));

        String system = PromptTemplates.judgeSystem();
        String user   = PromptTemplates.render(PromptTemplates.judgeUser(), vars);
        String response = client.chatProbe(system, user);
        String json = JsonExtractor.clean(response);
        return AlmaJackson.mapper().readValue(json, JudgeReport.class);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
