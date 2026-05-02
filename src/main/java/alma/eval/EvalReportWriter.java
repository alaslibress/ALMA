package alma.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EvalReportWriter {

    private static final Path REPORT_DIR = Paths.get("build", "reports");

    private EvalReportWriter() {
    }

    public static Map<String, double[]> aggregate(List<EvalRow> rows) {
        Map<String, double[]> bySubType = new LinkedHashMap<>();
        for (EvalRow row : rows) {
            String type = row.entry().subType() == null ? "unknown" : row.entry().subType();
            bySubType.computeIfAbsent(type, k -> new double[3]);
            double[] sums = bySubType.get(type);
            sums[0] += row.baselineScore();
            sums[1] += row.almaScore();
            sums[2] += 1;
        }
        double[] overall = new double[3];
        for (double[] sums : bySubType.values()) {
            overall[0] += sums[0];
            overall[1] += sums[1];
            overall[2] += sums[2];
        }
        bySubType.put("overall", overall);
        return bySubType;
    }

    public static Path writeMarkdown(List<EvalRow> rows) {
        try {
            Files.createDirectories(REPORT_DIR);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path report = REPORT_DIR.resolve("eval-" + stamp + ".md");
            Files.writeString(report, render(rows), StandardCharsets.UTF_8);
            return report;
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to write eval report", error);
        }
    }

    private static String render(List<EvalRow> rows) {
        Map<String, double[]> agg = aggregate(rows);
        StringBuilder md = new StringBuilder();
        md.append("# ALMA Eval Report\n\nGenerated: ").append(LocalDateTime.now()).append("\n\n");
        md.append("## Per sub-type summary\n\n");
        md.append("| sub-type | baseline | alma | delta | n |\n");
        md.append("|----------|----------|------|-------|---|\n");
        for (Map.Entry<String, double[]> entry : agg.entrySet()) {
            double[] sums = entry.getValue();
            double bAvg = sums[2] == 0 ? 0 : sums[0] / sums[2];
            double aAvg = sums[2] == 0 ? 0 : sums[1] / sums[2];
            md.append(String.format(Locale.ROOT,
                    "| %s | %.3f | %.3f | %+.3f | %d |%n",
                    entry.getKey(), bAvg, aAvg, aAvg - bAvg, (int) sums[2]));
        }
        md.append("\n## Per-entry detail\n\n");
        for (EvalRow row : rows) {
            appendEntry(md, row);
        }
        return md.toString();
    }

    private static void appendEntry(StringBuilder md, EvalRow row) {
        BenchmarkEntry entry = row.entry();
        md.append("### ").append(entry.id())
          .append(" (").append(entry.subType()).append(")\n\n");
        md.append("- Input: `").append(entry.input()).append("`\n");
        md.append("- Reference: `").append(String.join(" | ", entry.references())).append("`\n");
        md.append("- Baseline (`").append(String.format(Locale.ROOT, "%.3f", row.baselineScore()))
          .append("` ").append(row.baselineStatus())
          .append("): `").append(escape(row.baselineOutput())).append("`\n");
        md.append("- ALMA (`").append(String.format(Locale.ROOT, "%.3f", row.almaScore()))
          .append("` ").append(row.almaStatus())
          .append("): `").append(escape(row.almaOutput())).append("`\n");
        if (entry.notes() != null && !entry.notes().isBlank()) {
            md.append("- Notes: ").append(entry.notes()).append("\n");
        }
        if (row.almaTrace() != null && !row.almaTrace().isEmpty()) {
            md.append("- ALMA trace:\n");
            for (String line : row.almaTrace()) {
                md.append("    - `").append(escape(line)).append("`\n");
            }
        }
        md.append("\n");
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("`", "'").replace("\n", " ");
    }
}
