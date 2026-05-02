package alma.core.pipeline;

import alma.agents.Blueprint;
import alma.agents.EvalReport;
import alma.core.MutationException;
import alma.llm.AlmaJackson;
import alma.llm.JsonExtractor;
import alma.llm.OpenAiClient;
import alma.llm.PromptTemplates;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.Map;

public final class LlmBlueprintRefiner implements BlueprintRefiner {

    private final OpenAiClient client;

    public LlmBlueprintRefiner(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public Blueprint refine(Blueprint current, EvalReport report) throws MutationException {
        String system = PromptTemplates.refineSystem();
        String user = PromptTemplates.render(PromptTemplates.refineUser(), variables(current, report));

        String response = callLlm(system, user);
        return parse(response, system);
    }

    private static Map<String, String> variables(Blueprint current, EvalReport report) throws MutationException {
        Map<String, String> vars = new HashMap<>();
        try {
            vars.put("current_blueprint", AlmaJackson.mapper().writeValueAsString(current));
        } catch (Exception serError) {
            throw new MutationException("Failed to serialize current Blueprint", serError);
        }
        vars.put("failure_summaries", String.join("\n", report.failureSummaries()));
        return vars;
    }

    private String callLlm(String system, String user) throws MutationException {
        try {
            return client.chatProbe(system, user);
        } catch (Exception apiError) {
            throw new MutationException("Refine call failed: " + apiError.getMessage(), apiError);
        }
    }

    private Blueprint parse(String response, String system) throws MutationException {
        try {
            return JsonExtractor.parseOrThrow(response, AlmaJackson.mapper(), Blueprint.class);
        } catch (JsonProcessingException firstError) {
            String repaired = repair(response, system);
            try {
                return JsonExtractor.parseOrThrow(repaired, AlmaJackson.mapper(), Blueprint.class);
            } catch (JsonProcessingException secondError) {
                throw new MutationException(
                        "Refine JSON parse failed after repair retry: " + secondError.getMessage(),
                        secondError);
            }
        }
    }

    private String repair(String badOutput, String system) throws MutationException {
        String repairPrompt = "Your previous output was not valid JSON or did not match the schema.\n"
                + "Bad output:\n" + badOutput + "\n\n"
                + "Return ONLY the corrected Blueprint JSON now. No prose. No fences.";
        return callLlm(system, repairPrompt);
    }
}
