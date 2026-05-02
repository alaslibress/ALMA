package alma.llm;

import java.util.Collections;
import java.util.List;
import java.util.Set;

// Empty shell. Sub-Plan 05 supplies the real implementation.
public final class DummyToolRegistry implements ToolRegistry {

    @Override
    public Set<String> names() {
        return Collections.emptySet();
    }

    @Override
    public List<Object> resolve(List<String> names) {
        return Collections.emptyList();
    }
}
