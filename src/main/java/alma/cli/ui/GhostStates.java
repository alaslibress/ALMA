package alma.cli.ui;

public enum GhostStates {

    BASE("alma-base"),
    THINKING("alma-thinking"),
    PROBING("alma-probing"),
    MUTATING("alma-mutating"),
    SPEAKING("alma-speaking"),
    ERROR("alma-error");

    private final String resourceName;

    GhostStates(String resourceName) {
        this.resourceName = resourceName;
    }

    public String resourceName() {
        return resourceName;
    }

    public String render() {
        return AsciiBanner.load(resourceName);
    }

    public String color() {
        return switch (this) {
            case ERROR    -> AnsiPalette.c(AnsiPalette.ERR_RED);
            case MUTATING -> AnsiPalette.c(AnsiPalette.WARN_YELLOW);
            case SPEAKING -> AnsiPalette.c(AnsiPalette.ACCENT_CYAN);
            default       -> AnsiPalette.c(AnsiPalette.MUTED_GRAY);
        };
    }
}
