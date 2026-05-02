package alma.core.pipeline;

import alma.agents.Blueprint;
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

public final class BlueprintPhase implements Phase {

    private final OpenAiClient client;

    public BlueprintPhase(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "Blueprint";
    }

    @Override
    public void run(AgentContext context) throws MutationException {
        ProbeReport probe = context.probe();
        if (probe == null) {
            throw new MutationException("Blueprint phase requires a ProbeReport.");
        }

        String system = PromptTemplates.blueprintSystem();
        String user = PromptTemplates.render(PromptTemplates.blueprintUser(), variables(context, probe));

        String response = callLlm(system, user);
        Blueprint blueprint = parseBlueprint(response, system);
        context.setBlueprint(blueprint);

        System.out.println();
        System.out.println("  blueprint: " + blueprint.title());
        System.out.println("  tools:     " + blueprint.toolNames());
    }

    private static Map<String, String> variables(AgentContext context, ProbeReport probe) {
        Map<String, String> vars = new HashMap<>();
        vars.put("task_text", context.rawInput());
        try {
            vars.put("probe_report", AlmaJackson.mapper().writeValueAsString(probe));
        } catch (Exception serError) {
            throw new RuntimeException("Failed to serialize ProbeReport", serError);
        }
        return vars;
    }

    private String callLlm(String system, String user) throws MutationException {
        try {
            return client.chatProbe(system, user);
        } catch (Exception apiError) {
            throw new MutationException("OpenAI Blueprint call failed: " + apiError.getMessage(), apiError);
        }
    }

    private Blueprint parseBlueprint(String response, String system) throws MutationException {
        try {
            return JsonExtractor.parseOrThrow(response, AlmaJackson.mapper(), Blueprint.class);
        } catch (JsonProcessingException firstError) {
            String repaired = repair(response, system);
            try {
                return JsonExtractor.parseOrThrow(repaired, AlmaJackson.mapper(), Blueprint.class);
            } catch (JsonProcessingException secondError) {
                throw new MutationException(
                        "Blueprint JSON parse failed after repair retry: " + secondError.getMessage(),
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
