package alma.nlp;

import dev.langchain4j.agent.tool.Tool;
import opennlp.tools.tokenize.SimpleTokenizer;

public final class Tokenizer {

    private static final SimpleTokenizer DEFAULT = SimpleTokenizer.INSTANCE;

    @Tool("Splits a text into word-level tokens. Locale-agnostic.")
    public String[] tokenize(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return DEFAULT.tokenize(text);
    }
}
