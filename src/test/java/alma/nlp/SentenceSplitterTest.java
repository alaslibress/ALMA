package alma.nlp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class SentenceSplitterTest {

    private final LanguageDetector languageDetector = new LanguageDetector();
    private final SentenceSplitter splitter = new SentenceSplitter(languageDetector);

    @Test
    void splitsThreeEnglishSentences() {
        String[] sentences = splitter.sentenceSplit(
                "This is one. This is another. And this is the third.",
                "en"
        );
        assertThat(sentences).hasSize(3);
    }

    @Test
    void splitsSpanishParagraphHandlingOpeningPunctuation() {
        String[] sentences = splitter.sentenceSplit(
                "Hola amigo. ¿Cómo estás hoy? Estoy bien, gracias.",
                "es"
        );
        assertThat(sentences).hasSize(3);
    }

    @Test
    void singleArgOverloadDetectsLanguage() {
        String[] sentences = splitter.sentenceSplit(
                "Vou chegar atrasado. O trânsito está terrível. Já estou a caminho."
        );
        assertThat(sentences).hasSize(3);
    }

    @Test
    void emptyInputReturnsEmptyArray() {
        assertThat(splitter.sentenceSplit("")).isEmpty();
        assertThat(splitter.sentenceSplit(null)).isEmpty();
    }
}
