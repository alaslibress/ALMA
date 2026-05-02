package alma.nlp;

import alma.cli.ui.LanguageProvider;
import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObjectFactory;
import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public final class LanguageDetector implements LanguageProvider {

    private final com.optimaize.langdetect.LanguageDetector detector;
    private final TextObjectFactory textFactory;

    public LanguageDetector() {
        this.detector = buildDetector();
        this.textFactory = CommonTextObjectFactories.forDetectingShortCleanText();
    }

    @Tool("Detects the source language of a text. Returns ISO-639-1 code (en, es, pt, ...).")
    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "und";
        }
        List<DetectedLanguage> ranked = detector.getProbabilities(textFactory.forText(text));
        if (ranked.isEmpty()) {
            return "und";
        }
        return ranked.get(0).getLocale().getLanguage();
    }

    @Override
    public String sourceLanguage(String text) {
        return detectLanguage(text);
    }

    private static com.optimaize.langdetect.LanguageDetector buildDetector() {
        try {
            List<LanguageProfile> profiles = new LanguageProfileReader().readAllBuiltIn();
            return LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(profiles)
                    .build();
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to load language profiles", error);
        }
    }
}
