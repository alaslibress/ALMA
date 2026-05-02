package alma.llm;

import dev.langchain4j.model.output.TokenUsage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;

// Append-only CSV log of LLM token usage per request.
public final class TokenUsageLogger {

    private static final Path REPORT_DIR = Paths.get("build", "reports");
    private static final Path CSV_FILE = REPORT_DIR.resolve("token-usage.csv");
    private static final String HEADER =
            "timestamp,model,input_tokens,output_tokens,total_tokens,est_usd\n";

    private static final Object FILE_LOCK = new Object();
    private static volatile boolean headerWritten = false;

    private TokenUsageLogger() {
    }

    public static void record(String model, TokenUsage usage) {
        if (usage == null) {
            return;
        }
        int input = usage.inputTokenCount() == null ? 0 : usage.inputTokenCount();
        int output = usage.outputTokenCount() == null ? 0 : usage.outputTokenCount();
        int total = usage.totalTokenCount() == null ? input + output : usage.totalTokenCount();
        double estUsd = estimateCost(model, input, output);

        String line = String.format(Locale.ROOT,
                "%s,%s,%d,%d,%d,%.6f%n",
                Instant.now(), model, input, output, total, estUsd);

        appendLine(line);
    }

    private static void appendLine(String line) {
        synchronized (FILE_LOCK) {
            try {
                Files.createDirectories(REPORT_DIR);
                if (!headerWritten && !Files.exists(CSV_FILE)) {
                    Files.writeString(CSV_FILE, HEADER, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
                headerWritten = true;
                Files.writeString(CSV_FILE, line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ignored) {
                // Logging failure must not break the run.
            }
        }
    }

    // Approximate per-1M-token prices, USD. Update if OpenAI changes pricing.
    private static double estimateCost(String model, int inputTokens, int outputTokens) {
        double inPer1M;
        double outPer1M;
        if (model == null) {
            return 0.0;
        }
        if (model.startsWith("gpt-4o-mini")) {
            inPer1M = 0.15;
            outPer1M = 0.60;
        } else if (model.startsWith("gpt-4o")) {
            inPer1M = 2.50;
            outPer1M = 10.00;
        } else {
            return 0.0;
        }
        return (inputTokens / 1_000_000.0) * inPer1M
                + (outputTokens / 1_000_000.0) * outPer1M;
    }
}
