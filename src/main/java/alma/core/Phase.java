package alma.core;

public interface Phase {

    String name();

    void run(AgentContext context) throws MutationException;
}
