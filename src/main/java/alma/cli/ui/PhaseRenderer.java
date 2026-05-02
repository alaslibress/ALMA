package alma.cli.ui;

import java.io.PrintStream;

public final class PhaseRenderer {

    private static final int PADDED_WIDTH = 42;

    private final PrintStream out;

    public PhaseRenderer(PrintStream out) {
        this.out = out;
    }

    public void phaseStart(int index, int total, String name) {
        String prefix = String.format("| [%d/%d] %s ", index, total, name);
        StringBuilder dots = new StringBuilder(prefix);
        while (dots.length() < PADDED_WIDTH) {
            dots.append('.');
        }
        out.print(AnsiPalette.c(AnsiPalette.MUTED_GRAY) + dots + " "
                + AnsiPalette.c(AnsiPalette.RESET));
        out.flush();
    }

    public void phaseOk() {
        out.println(AnsiPalette.c(AnsiPalette.OK_GREEN) + "* ok"
                + AnsiPalette.c(AnsiPalette.RESET));
    }

    public void phaseFail(String message) {
        out.println(AnsiPalette.c(AnsiPalette.ERR_RED) + "* FAIL: " + message
                + AnsiPalette.c(AnsiPalette.RESET));
    }

    public void streamLine(String line) {
        out.println(line);
    }
}
