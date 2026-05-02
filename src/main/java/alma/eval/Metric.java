package alma.eval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Character n-gram F-score (Popović 2015). N=6, beta=2.
// Values in [0, 1]. 1.0 means perfect match against the closest reference.
public final class Metric {

    private static final int N = 6;
    private static final double BETA = 2.0;

    private Metric() {
    }

    public static double chrF(String hypothesis, List<String> references) {
        if (hypothesis == null || hypothesis.isEmpty()) return 0.0;
        if (references == null || references.isEmpty()) return 0.0;

        double best = 0.0;
        for (String reference : references) {
            if (reference == null || reference.isEmpty()) continue;
            double f = chrFAgainstOne(hypothesis, reference);
            if (f > best) best = f;
        }
        return best;
    }

    private static double chrFAgainstOne(String hypothesis, String reference) {
        double sumPrecision = 0.0;
        double sumRecall = 0.0;
        int validNs = 0;

        for (int n = 1; n <= N; n++) {
            Map<String, Integer> hypGrams = ngrams(hypothesis, n);
            Map<String, Integer> refGrams = ngrams(reference, n);

            int matches = countMatches(hypGrams, refGrams);
            int hypTotal = sumValues(hypGrams);
            int refTotal = sumValues(refGrams);

            if (hypTotal == 0 && refTotal == 0) continue;
            double precision = hypTotal == 0 ? 0.0 : (double) matches / hypTotal;
            double recall    = refTotal == 0 ? 0.0 : (double) matches / refTotal;

            sumPrecision += precision;
            sumRecall    += recall;
            validNs++;
        }

        if (validNs == 0) return 0.0;
        double avgP = sumPrecision / validNs;
        double avgR = sumRecall    / validNs;
        if (avgP == 0.0 && avgR == 0.0) return 0.0;

        double beta2 = BETA * BETA;
        return (1 + beta2) * (avgP * avgR) / (beta2 * avgP + avgR);
    }

    private static Map<String, Integer> ngrams(String text, int n) {
        Map<String, Integer> map = new HashMap<>();
        if (text.length() < n) return map;
        for (int i = 0; i <= text.length() - n; i++) {
            String gram = text.substring(i, i + n);
            map.merge(gram, 1, Integer::sum);
        }
        return map;
    }

    private static int countMatches(Map<String, Integer> a, Map<String, Integer> b) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : a.entrySet()) {
            int otherCount = b.getOrDefault(entry.getKey(), 0);
            total += Math.min(entry.getValue(), otherCount);
        }
        return total;
    }

    private static int sumValues(Map<String, Integer> map) {
        int total = 0;
        for (int value : map.values()) total += value;
        return total;
    }
}
