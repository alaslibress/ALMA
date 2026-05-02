package alma.agents;

import java.util.List;

public record EvalReport(
        List<TaskScore> taskScores,
        double average,
        List<String> failureSummaries
) {
}
