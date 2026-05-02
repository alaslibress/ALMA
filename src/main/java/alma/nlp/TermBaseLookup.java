package alma.nlp;

import alma.llm.AlmaJackson;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;

public final class TermBaseLookup {

    private static final String RESOURCE = "termbase.json";

    // Layout: from -> to -> term -> translation.
    private final Map<String, Map<String, Map<String, String>>> table;

    public TermBaseLookup() {
        this.table = loadFromClasspath(RESOURCE);
    }

    @Tool("Looks up a fixed translation for a term given source and target language ISO codes. Returns empty string if unknown.")
    public String lookupTermBase(String fromLang, String toLang, String term) {
        if (fromLang == null || toLang == null || term == null) {
            return "";
        }
        Map<String, Map<String, String>> byTo = table.get(fromLang.toLowerCase());
        if (byTo == null) {
            return "";
        }
        Map<String, String> entries = byTo.get(toLang.toLowerCase());
        if (entries == null) {
            return "";
        }
        return entries.getOrDefault(term, "");
    }

    private static Map<String, Map<String, Map<String, String>>> loadFromClasspath(String resource) {
        try (InputStream stream = TermBaseLookup.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return Collections.emptyMap();
            }
            return AlmaJackson.mapper().readValue(
                    stream,
                    new TypeReference<Map<String, Map<String, Map<String, String>>>>() {}
            );
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to load " + resource, error);
        }
    }
}
