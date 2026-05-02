package alma.cli.ui;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class LanguageBadge {

    private static final String UNICODE_ARROW = "➜";   // right-pointing arrow
    private static final String ASCII_ARROW   = "->";

    private final LanguageProvider provider;
    private final String arrow;

    public LanguageBadge(LanguageProvider provider) {
        this.provider = provider;
        this.arrow = pickArrow();
    }

    public String render(String rawInput, String targetLanguage) {
        String source = provider.sourceLanguage(rawInput).toUpperCase();
        String target = (targetLanguage == null || targetLanguage.isBlank())
                ? "??"
                : targetLanguage.toUpperCase();

        return AnsiPalette.c(AnsiPalette.DIM) + "[ "
                + AnsiPalette.c(AnsiPalette.ACCENT_CYAN) + source
                + AnsiPalette.c(AnsiPalette.DIM) + " " + arrow + " "
                + AnsiPalette.c(AnsiPalette.ACCENT_MAGENTA) + target
                + AnsiPalette.c(AnsiPalette.DIM) + " ]"
                + AnsiPalette.c(AnsiPalette.RESET);
    }

    public void print(PrintStream out, String rawInput, String targetLanguage) {
        out.println("  " + render(rawInput, targetLanguage));
    }

    // Pick `->` whenever colors are off OR stdout encoding is not UTF-8.
    private static String pickArrow() {
        if (!AnsiPalette.isEnabled()) {
            return ASCII_ARROW;
        }
        if (!isUtf8(stdoutEncoding())) {
            return ASCII_ARROW;
        }
        return UNICODE_ARROW;
    }

    private static String stdoutEncoding() {
        String value = System.getProperty("stdout.encoding");
        if (value != null) return value;
        value = System.getProperty("sun.stdout.encoding");
        if (value != null) return value;
        value = System.getProperty("native.encoding");
        if (value != null) return value;
        value = System.getProperty("file.encoding");
        return value != null ? value : StandardCharsets.UTF_8.name();
    }

    private static boolean isUtf8(String encoding) {
        if (encoding == null) return false;
        String normalized = encoding.toUpperCase().replace("-", "");
        return "UTF8".equals(normalized);
    }
}
