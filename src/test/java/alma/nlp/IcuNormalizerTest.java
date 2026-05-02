package alma.nlp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class IcuNormalizerTest {

    private final IcuNormalizer normalizer = new IcuNormalizer();

    @Test
    void normalizesPrecomposedAndDecomposedToSameForm() {
        String composed   = "café";          // pre-composed `é`
        String decomposed = "café";     // `e` + combining acute
        assertThat(normalizer.normalizeUnicode(decomposed)).isEqualTo(composed);
        assertThat(normalizer.normalizeUnicode(composed)).isEqualTo(composed);
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertThat(normalizer.normalizeUnicode("")).isEmpty();
        assertThat(normalizer.normalizeUnicode(null)).isEmpty();
    }
}
