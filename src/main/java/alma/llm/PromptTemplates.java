package alma.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class PromptTemplates {

    private static final String DELIM = "=== USER ===";

    private static final String[] PROBE     = loadAndSplit("prompts/probe.txt");
    private static final String[] BLUEPRINT = loadAndSplit("prompts/blueprint.txt");
    private static final String[] REFINE    = loadAndSplit("prompts/refine.txt");
    private static final String[] JUDGE     = loadAndSplit("prompts/judge.txt");

    private PromptTemplates() {
    }

    public static String probeSystem()     { return PROBE[0]; }
    public static String probeUser()       { return PROBE[1]; }

    public static String blueprintSystem() { return BLUEPRINT[0]; }
    public static String blueprintUser()   { return BLUEPRINT[1]; }

    public static String refineSystem()    { return REFINE[0]; }
    public static String refineUser()      { return REFINE[1]; }

    public static String judgeSystem()     { return JUDGE[0]; }
    public static String judgeUser()       { return JUDGE[1]; }

    public static String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String token = "{{ " + entry.getKey() + " }}";
            result = result.replace(token, entry.getValue());
        }
        return result;
    }

    private static String[] loadAndSplit(String resource) {
        String body = readUtf8(resource);
        int delim = body.indexOf(DELIM);
        if (delim < 0) {
            throw new IllegalStateException("Prompt template missing '" + DELIM + "' marker: " + resource);
        }
        String system = stripSystemHeader(body.substring(0, delim));
        String user = body.substring(delim + DELIM.length()).strip();
        return new String[] { system.strip(), user };
    }

    private static String readUtf8(String resource) {
        try (InputStream stream = PromptTemplates.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing prompt resource: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to read " + resource, error);
        }
    }

    private static String stripSystemHeader(String section) {
        String marker = "=== SYSTEM ===";
        int idx = section.indexOf(marker);
        if (idx < 0) {
            return section;
        }
        return section.substring(idx + marker.length());
    }
}
