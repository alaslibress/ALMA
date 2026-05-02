package alma.core.pipeline;

import alma.core.AgentContext;
import alma.core.Phase;

public final class ReportPhase implements Phase {

    @Override
    public String name() {
        return "Report";
    }

    @Override
    public void run(AgentContext context) {
        System.out.println();
        System.out.println("---");

        String result = context.result();
        if (result == null || result.isBlank()) {
            result = "[stub]";
        }
        System.out.println(result);

        int rounds = context.evolutionTrace().size();
        if (rounds > 0) {
            System.out.println("(rounds: " + rounds + ")");
        }
    }
}
