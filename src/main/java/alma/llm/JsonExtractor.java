package alma.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonExtractor {

    private JsonExtractor() {
    }

    // Strips Markdown fences and prose around a JSON object.
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            String text = raw.strip();
            if (text.startsWith("```")) {
                int firstNewline = text.indexOf('\n');
                text = firstNewline > 0 ? text.substring(firstNewline + 1) : text.substring(3);
                int closingFence = text.lastIndexOf("```");
                if (closingFence >= 0) {
                    text = text.substring(0, closingFence);
                }
                text = text.strip();
            }
            int firstBrace = text.indexOf('{');
            int lastBrace = text.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return text.substring(firstBrace, lastBrace + 1);
            }
            return text;
        } catch (RuntimeException stringMathError) {
            return raw.strip();
        }
    }

    // Clean then bind in one call.
    public static <T> T parseOrThrow(String raw, ObjectMapper mapper, Class<T> type)
            throws JsonProcessingException {
        return mapper.readValue(clean(raw), type);
    }
}
