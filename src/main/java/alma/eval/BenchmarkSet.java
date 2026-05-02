package alma.eval;

import alma.llm.AlmaJackson;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

public final class BenchmarkSet {

    private BenchmarkSet() {
    }

    public static List<BenchmarkEntry> load(String resourcePath) {
        try (InputStream stream = BenchmarkSet.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Collections.emptyList();
            }
            return AlmaJackson.mapper().readValue(stream, new TypeReference<List<BenchmarkEntry>>() {});
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to load benchmark: " + resourcePath, error);
        }
    }
}
