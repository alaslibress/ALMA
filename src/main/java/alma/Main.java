package alma;

import alma.cli.AlmaCommand;
import alma.config.AlmaConfig;
import alma.config.EnvLoader;
import alma.config.MissingEnvException;
import picocli.CommandLine;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        AlmaConfig config = EnvLoader.load();

        if (needsApiKey(args)) {
            try {
                EnvLoader.requireOpenAiKey(config);
            } catch (MissingEnvException missing) {
                System.err.println("ALMA: " + missing.getMessage());
                System.exit(1);
                return;
            }
        }

        int exitCode = new CommandLine(new AlmaCommand(config)).execute(args);
        System.exit(exitCode);
    }

    // Key needed only for commands that hit OpenAI.
    private static boolean needsApiKey(String[] args) {
        for (String arg : args) {
            if (arg.equals("run") || arg.equals("evaluate")) {
                return true;
            }
        }
        return false;
    }
}
