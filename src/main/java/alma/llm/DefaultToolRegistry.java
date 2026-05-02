package alma.llm;

import alma.nlp.IcuNormalizer;
import alma.nlp.LanguageDetector;
import alma.nlp.SentenceSplitter;
import alma.nlp.TermBaseLookup;
import alma.nlp.Tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DefaultToolRegistry implements ToolRegistry {

    private static final Set<String> AVAILABLE = Set.of(
            "detectLanguage",
            "tokenize",
            "sentenceSplit",
            "normalizeUnicode",
            "lookupTermBase"
    );

    private final LanguageDetector languageDetector;
    private final Tokenizer tokenizer;
    private final SentenceSplitter sentenceSplitter;
    private final IcuNormalizer icuNormalizer;
    private final TermBaseLookup termBase;

    public DefaultToolRegistry() {
        this.languageDetector = new LanguageDetector();
        this.tokenizer = new Tokenizer();
        this.sentenceSplitter = new SentenceSplitter(languageDetector);
        this.icuNormalizer = new IcuNormalizer();
        this.termBase = new TermBaseLookup();
    }

    @Override
    public Set<String> names() {
        return Collections.unmodifiableSet(AVAILABLE);
    }

    @Override
    public List<Object> resolve(List<String> names) {
        Set<Object> instances = new LinkedHashSet<>();
        for (String name : names) {
            if (!AVAILABLE.contains(name)) {
                throw new IllegalArgumentException("Unknown tool name: " + name);
            }
            instances.add(instanceFor(name));
        }
        return new ArrayList<>(instances);
    }

    public LanguageDetector languageDetector() {
        return languageDetector;
    }

    private Object instanceFor(String name) {
        return switch (name) {
            case "detectLanguage"   -> languageDetector;
            case "tokenize"         -> tokenizer;
            case "sentenceSplit"    -> sentenceSplitter;
            case "normalizeUnicode" -> icuNormalizer;
            case "lookupTermBase"   -> termBase;
            default -> throw new IllegalStateException("Unhandled tool: " + name);
        };
    }
}
