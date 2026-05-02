package alma.eval;

import java.util.Map;

public record JudgeReport(
        String reasoning,
        Map<String, Double> scores,
        double overall
) {
}
