package alma.eval;

import java.util.List;

public record BenchmarkEntry(
        String id,
        String subType,
        String input,
        List<String> references,
        String notes
) {
}
