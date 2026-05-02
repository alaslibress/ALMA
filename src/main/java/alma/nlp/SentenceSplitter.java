package alma.nlp;

import dev.langchain4j.agent.tool.Tool;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class SentenceSplitter {

    private static final String DEFAULT_LANGUAGE = "en";

    private final LanguageDetector languageDetector;
    private final Map<String, SentenceDetectorME> detectors;

    public SentenceSplitter(LanguageDetector languageDetector) {
        this.languageDetector = languageDetector;
        this.detectors = loadAllModels();
    }

    @Tool("Splits a text into sentences. Auto-detects the language if not provided.")
    public String[] sentenceSplit(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        String language = languageDetector.detectLanguage(text);
        return splitWith(detectorFor(language), text);
    }

    public String[] sentenceSplit(String text, String language) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return splitWith(detectorFor(language), text);
    }

    private SentenceDetectorME detectorFor(String language) {
        SentenceDetectorME exact = detectors.get(language);
        if (exact != null) {
            return exact;
        }
        return detectors.get(DEFAULT_LANGUAGE);
    }

    private static String[] splitWith(SentenceDetectorME detector, String text) {
        // Detector can be called from many threads if we ever go concurrent.
        synchronized (detector) {
            return detector.sentDetect(text);
        }
    }

    private static Map<String, SentenceDetectorME> loadAllModels() {
        Map<String, SentenceDetectorME> map = new HashMap<>();
        map.put("en", load("nlp/en-sent.bin"));
        map.put("es", load("nlp/es-sent.bin"));
        map.put("pt", load("nlp/pt-sent.bin"));
        return map;
    }

    private static SentenceDetectorME load(String resourcePath) {
        try (InputStream stream = SentenceSplitter.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing OpenNLP model: " + resourcePath);
            }
            SentenceModel model = new SentenceModel(stream);
            return new SentenceDetectorME(model);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to load " + resourcePath, error);
        }
    }
}
