package alma.nlp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class LanguageDetectorTest {

    private final LanguageDetector detector = new LanguageDetector();

    @Test
    void detectsEnglishSentences() {
        assertThat(detector.detectLanguage("This is a longer English sentence about software engineering.")).isEqualTo("en");
        assertThat(detector.detectLanguage("The quick brown fox jumps over the lazy dog every morning.")).isEqualTo("en");
        assertThat(detector.detectLanguage("Hello there, how are you doing on this fine Tuesday afternoon?")).isEqualTo("en");
    }

    @Test
    void detectsSpanishSentences() {
        assertThat(detector.detectLanguage("Hoy hace mucho calor en la ciudad y queremos ir a la playa.")).isEqualTo("es");
        assertThat(detector.detectLanguage("¿Podrías pasarme la sal, por favor? Esta sopa necesita un poco más.")).isEqualTo("es");
        assertThat(detector.detectLanguage("El código abierto permite a la comunidad mejorar las herramientas que usamos.")).isEqualTo("es");
    }

    @Test
    void detectsPortugueseSentences() {
        assertThat(detector.detectLanguage("Vou chegar atrasado por causa do trânsito intenso na cidade hoje.")).isEqualTo("pt");
        assertThat(detector.detectLanguage("O código aberto permite que a comunidade melhore as ferramentas que usamos.")).isEqualTo("pt");
        assertThat(detector.detectLanguage("Adoro caminhar pela praia ao pôr do sol durante o verão tropical.")).isEqualTo("pt");
    }

    @Test
    void returnsUndForBlank() {
        assertThat(detector.detectLanguage("")).isEqualTo("und");
        assertThat(detector.detectLanguage(null)).isEqualTo("und");
    }
}
