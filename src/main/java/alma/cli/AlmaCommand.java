package alma.cli;

import alma.cli.ui.AnsiPalette;
import alma.cli.ui.AsciiBanner;
import alma.config.AlmaConfig;
import alma.config.EnvLoader;
import alma.core.StemCell;
import alma.eval.EvalHarness;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
        name = "alma",
        mixinStandardHelpOptions = true,
        versionProvider = AlmaVersionProvider.class,
        description = "ALMA — Adaptive Linguistic Metamorphosis Agent.",
        subcommands = {
                AlmaCommand.RunCommand.class,
                AlmaCommand.EvaluateCommand.class,
                AlmaCommand.VersionCommand.class
        }
)
public final class AlmaCommand implements Runnable {

    private final AlmaConfig config;

    public AlmaCommand(AlmaConfig config) {
        this.config = config;
    }

    public AlmaConfig config() {
        return config;
    }

    @Override
    public void run() {
        AsciiBanner.print(System.out, "alma-base");
        new CommandLine(this).usage(System.out);
    }

    @Command(name = "run", description = "Run ALMA on a single linguistic task.")
    public static final class RunCommand implements Callable<Integer> {

        @ParentCommand
        private AlmaCommand parent;

        @Parameters(index = "0", description = "Task text in quotes.")
        private String task;

        @Option(names = "--lang", description = "ISO-639-1 target language (en, es, pt). Optional.")
        private String targetLanguage;

        @Option(names = "--no-color", description = "Strip ANSI colors and Unicode arrows.")
        private boolean noColor;

        @Override
        public Integer call() {
            if (noColor) {
                AnsiPalette.setEnabled(false);
            }
            AsciiBanner.print(System.out, "alma-base");
            return new StemCell(parent.config(), targetLanguage).run(task);
        }
    }

    @Command(name = "evaluate", description = "Run baseline vs ALMA on the benchmark.")
    public static final class EvaluateCommand implements Callable<Integer> {

        @ParentCommand
        private AlmaCommand parent;

        @Option(names = "--no-color", description = "Strip ANSI colors and Unicode arrows.")
        private boolean noColor;

        @Override
        public Integer call() {
            if (noColor) {
                AnsiPalette.setEnabled(false);
            }
            AsciiBanner.print(System.out, "alma-base");
            return new EvalHarness(parent.config()).run(System.out);
        }
    }

    @Command(name = "version", description = "Print ALMA version.")
    public static final class VersionCommand implements Runnable {

        @Override
        public void run() {
            AlmaConfig config = EnvLoader.load();
            System.out.println("ALMA " + config.version());
        }
    }
}
