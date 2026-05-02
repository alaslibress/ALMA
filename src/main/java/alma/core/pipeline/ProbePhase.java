package alma.core.pipeline;

import alma.agents.ProbeReport;
import alma.core.AgentContext;
import alma.core.MutationException;
import alma.core.Phase;
import alma.llm.AlmaJackson;
import alma.llm.JsonExtractor;
import alma.llm.OpenAiClient;
import alma.llm.PromptTemplates;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.Map;

public final class ProbePhase implements Phase {

    private final OpenAiClient client;

    public ProbePhase(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "Probe";
    }

    @Override
    public void run(AgentContext context) throws MutationException {
        String system = PromptTemplates.probeSystem();
        String user = PromptTemplates.render(PromptTemplates.probeUser(), variables(context));

        String response = callLlm(system, user);
        ProbeReport report = parseReport(response, system);
        context.setProbe(report);
    }

    private static Map<String, String> variables(AgentContext context) {
        Map<String, String> vars = new HashMap<>();
        vars.put("task_text", context.rawInput());
        vars.put("class_hint", context.classHint() != null
                ? context.classHint()
                : "linguistic transformation");
        return vars;
    }

    private String callLlm(String system, String user) throws MutationException {
        try {
            return client.chatProbe(system, user);
        } catch (Exception apiError) {
            throw new MutationException("OpenAI Probe call failed: " + apiError.getMessage(), apiError);
        }
    }

    private ProbeReport parseReport(String response, String system) throws MutationException {
        try {
            return JsonExtractor.parseOrThrow(response, AlmaJackson.mapper(), ProbeReport.class);
        } catch (JsonProcessingException firstError) {
            String repaired = repair(response, system);
            try {
                return JsonExtractor.parseOrThrow(repaired, AlmaJackson.mapper(), ProbeReport.class);
            } catch (JsonProcessingException secondError) {
                throw new MutationException(
                        "Probe JSON parse failed after repair retry: " + secondError.getMessage(),
                        secondError);
            }
        }
    }

    private String repair(String badOutput, String system) throws MutationException {
        String repairPrompt = "Your previous output was not valid JSON or did not match the schema.\n"
                + "Bad output:\n" + badOutput + "\n\n"
                + "Return ONLY the corrected JSON now. No prose. No fences.";
        return callLlm(system, repairPrompt);
    }
}
