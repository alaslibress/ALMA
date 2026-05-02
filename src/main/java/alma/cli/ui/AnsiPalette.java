package alma.cli.ui;

public final class AnsiPalette {

    public static final String RESET           = "[0m";
    public static final String BOLD            = "[1m";
    public static final String DIM             = "[2m";
    public static final String OK_GREEN        = "[32m";
    public static final String WARN_YELLOW     = "[33m";
    public static final String ERR_RED         = "[31m";
    public static final String MUTED_GRAY      = "[90m";
    public static final String ACCENT_CYAN     = "[36m";
    public static final String ACCENT_MAGENTA  = "[35m";

    private static volatile boolean enabled = true;

    private AnsiPalette() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // Returns the code only when colors are enabled. Empty string otherwise.
    public static String c(String code) {
        return enabled ? code : "";
    }
}
