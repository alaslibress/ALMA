package alma.agents;

import java.util.List;

public record ProbeReport(
        List<String> subSkills,
        List<String> suggestedToolNames,
        List<EvalCriterion> criteria,
        List<EvalTaskSeed> syntheticTasks,
        String notes
) {
}
