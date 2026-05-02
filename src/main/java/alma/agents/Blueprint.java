package alma.agents;

import java.util.List;

public record Blueprint(
        String title,
        String systemPrompt,
        List<String> toolNames,
        List<EvalCriterion> criteria,
        String modelName,
        double scoreThreshold,
        int maxRefinementRounds
) {
}
