package alma.nlp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class TermBaseLookupTest {

    private final TermBaseLookup termBase = new TermBaseLookup();

    @Test
    void findsKnownEnToEsTerm() {
        assertThat(termBase.lookupTermBase("en", "es", "open source")).isEqualTo("código abierto");
    }

    @Test
    void findsKnownEnToPtTerm() {
        assertThat(termBase.lookupTermBase("en", "pt", "deadline")).isEqualTo("prazo final");
    }

    @Test
    void unknownTermReturnsEmpty() {
        assertThat(termBase.lookupTermBase("en", "es", "nonexistent term")).isEmpty();
    }

    @Test
    void unknownLanguagePairReturnsEmpty() {
        assertThat(termBase.lookupTermBase("ru", "es", "open source")).isEmpty();
    }

    @Test
    void nullsAreSafe() {
        assertThat(termBase.lookupTermBase(null, "es", "x")).isEmpty();
        assertThat(termBase.lookupTermBase("en", null, "x")).isEmpty();
        assertThat(termBase.lookupTermBase("en", "es", null)).isEmpty();
    }
}
