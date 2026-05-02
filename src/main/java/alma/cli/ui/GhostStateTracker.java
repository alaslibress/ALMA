package alma.cli.ui;

import java.io.PrintStream;

// Render once per state CHANGE. Suppresses scroll spam when phases share a state.
public final class GhostStateTracker {

    private final PrintStream out;
    private GhostStates last;

    public GhostStateTracker(PrintStream out) {
        this.out = out;
    }

    // Tells the tracker the current state without printing.
    // Useful when AlmaCommand already printed the BASE banner at boot.
    public void markInitial(GhostStates state) {
        this.last = state;
    }

    public void show(GhostStates state) {
        if (state == last) {
            return;
        }
        last = state;
        out.println();
        out.print(state.color());
        out.print(state.render());
        out.println(AnsiPalette.c(AnsiPalette.RESET));
    }
}
