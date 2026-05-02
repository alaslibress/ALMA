package alma.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

final class MetricChrFTest {

    @Test
    void identicalStringsReturnOne() {
        double f = Metric.chrF("hello", List.of("hello"));
        assertThat(f).isCloseTo(1.0, within(0.001));
    }

    @Test
    void emptyHypothesisReturnsZero() {
        assertThat(Metric.chrF("", List.of("hola mundo"))).isZero();
        assertThat(Metric.chrF(null, List.of("hola mundo"))).isZero();
    }

    @Test
    void noOverlapReturnsZero() {
        // No shared character n-grams up to N=6.
        double f = Metric.chrF("abc", List.of("xyz"));
        assertThat(f).isCloseTo(0.0, within(0.001));
    }

    @Test
    void partialMatchAgainstHandComputed() {
        // hyp = "abc", ref = "abd"
        // n=1: matches=2, total=3 each -> p=r=2/3
        // n=2: matches=1, total=2 each -> p=r=1/2
        // n=3: matches=0, total=1 each -> p=r=0
        // n=4..6: skipped (text shorter than n)
        // avgP = avgR = (2/3 + 1/2 + 0) / 3 = 7/18
        // beta=2, F = (1+4)*p*r / (4*p + r) = with p == r, F = p = r
        // expected: 7/18 ~= 0.3889
        double f = Metric.chrF("abc", List.of("abd"));
        assertThat(f).isCloseTo(7.0 / 18.0, within(0.001));
    }

    @Test
    void picksBestReferenceWhenSeveralProvided() {
        double f = Metric.chrF("hola", List.of("nope", "hola"));
        assertThat(f).isCloseTo(1.0, within(0.001));
    }
}
