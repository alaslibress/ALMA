package alma.cli;

import alma.config.AlmaConfig;
import alma.config.EnvLoader;
import picocli.CommandLine;

public final class AlmaVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        AlmaConfig config = EnvLoader.load();
        return new String[] { "ALMA " + config.version() };
    }
}
