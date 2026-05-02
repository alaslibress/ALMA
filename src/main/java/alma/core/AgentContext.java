package alma.core;

import alma.agents.Blueprint;
import alma.agents.ProbeReport;
import alma.agents.SpecializedAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Mutable carrier passed phase to phase.
public final class AgentContext {

    private String rawInput;
    private String classHint;
    private ProbeReport probe;
    private Blueprint blueprint;
    private Blueprint lastGoodBlueprint;
    private SpecializedAgent lastGoodAgent;
    private SpecializedAgent agent;
    private String result;
    private final List<String> evolutionTrace = new ArrayList<>();

    public String rawInput() { return rawInput; }
    public void setRawInput(String value) { this.rawInput = value; }

    public String classHint() { return classHint; }
    public void setClassHint(String value) { this.classHint = value; }

    public ProbeReport probe() { return probe; }
    public void setProbe(ProbeReport value) { this.probe = value; }

    public Blueprint blueprint() { return blueprint; }
    public void setBlueprint(Blueprint value) { this.blueprint = value; }

    public Blueprint lastGoodBlueprint() { return lastGoodBlueprint; }
    public void setLastGoodBlueprint(Blueprint value) { this.lastGoodBlueprint = value; }

    public SpecializedAgent lastGoodAgent() { return lastGoodAgent; }
    public void setLastGoodAgent(SpecializedAgent value) { this.lastGoodAgent = value; }

    public SpecializedAgent agent() { return agent; }
    public void setAgent(SpecializedAgent value) { this.agent = value; }

    public String result() { return result; }
    public void setResult(String value) { this.result = value; }

    public List<String> evolutionTrace() { return Collections.unmodifiableList(evolutionTrace); }
    public void appendTrace(String line) { this.evolutionTrace.add(line); }

    // Atomic Blueprint+Agent promotion.
    public void promote(Blueprint newBlueprint, SpecializedAgent newAgent,
                        Blueprint demotedBlueprint, SpecializedAgent demotedAgent) {
        if (newBlueprint == null || newAgent == null) {
            throw new IllegalStateException("promote requires non-null Blueprint and Agent.");
        }
        this.lastGoodBlueprint = demotedBlueprint;
        this.lastGoodAgent = demotedAgent;
        this.blueprint = newBlueprint;
        this.agent = newAgent;
    }

    // Atomic rollback to last known-good pair.
    public void rollback() {
        if ((lastGoodBlueprint == null) != (lastGoodAgent == null)) {
            throw new IllegalStateException(
                    "rollback invariant broken: blueprint/agent presence diverged.");
        }
        if (lastGoodBlueprint == null) {
            return;
        }
        this.blueprint = lastGoodBlueprint;
        this.agent = lastGoodAgent;
    }
}
