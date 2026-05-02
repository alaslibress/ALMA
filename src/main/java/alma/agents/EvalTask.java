package alma.agents;

import java.util.List;

public record EvalTask(String id, String input, List<String> references) {
}
