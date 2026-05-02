package alma.nlp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class TokenizerTest {

    private final Tokenizer tokenizer = new Tokenizer();

    @Test
    void splitsWordsAndPunctuation() {
        String[] tokens = tokenizer.tokenize("Hello, world!");
        assertThat(tokens).containsExactly("Hello", ",", "world", "!");
    }

    @Test
    void emptyInputReturnsEmptyArray() {
        assertThat(tokenizer.tokenize("")).isEmpty();
        assertThat(tokenizer.tokenize(null)).isEmpty();
    }
}
