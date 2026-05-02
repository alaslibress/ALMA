package alma.config;

public record AlmaConfig(
        String version,
        String openAiApiKey,
        String modelProbe,
        String modelExec,
        String logLevel,
        String benchmarkPath
) {
}
