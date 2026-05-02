package alma.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public final class AlmaJackson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new ParameterNamesModule())
            .build();

    private AlmaJackson() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
