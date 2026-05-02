package alma.eval;

import java.util.List;

public record EvalRow(
        BenchmarkEntry entry,
        double baselineScore,
        double almaScore,
        String baselineOutput,
        String almaOutput,
        RunStatus baselineStatus,
        RunStatus almaStatus,
        List<String> almaTrace
) {

    public double delta() {
        return almaScore - baselineScore;
    }
}
