package alma.agents;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class SpecializedAgentExecuteTest {

    @Test
    void exposesNameFromBlueprint() {
        Blueprint blueprint = new Blueprint(
                "My Title",
                "system prompt",
                List.of(),
                List.of(),
                "gpt-4o",
                0.75,
                3
        );

        SpecializedAgent agent = new SpecializedAgent(blueprint, null, null);

        assertThat(agent.name()).isEqualTo("My Title");
        assertThat(agent.blueprint()).isSameAs(blueprint);
    }

    @Test
    void executeIsOverridable() {
        Blueprint blueprint = new Blueprint(
                "T", "sys", List.of(), List.of(), "gpt-4o", 0.5, 3);

        SpecializedAgent fixed = new SpecializedAgent(blueprint, null, null) {
            @Override
            public String execute(String input) {
                return "FIXED:" + input;
            }
        };

        assertThat(fixed.execute("hi")).isEqualTo("FIXED:hi");
    }
}
