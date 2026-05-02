package alma.llm;

import java.util.List;
import java.util.Set;

public interface ToolRegistry {

    Set<String> names();

    List<Object> resolve(List<String> names);
}
