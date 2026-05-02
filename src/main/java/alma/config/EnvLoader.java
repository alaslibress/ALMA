package alma.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class EnvLoader {

    private static final String PROPERTIES_PATH = "alma.properties";

    private EnvLoader() {
    }

    public static AlmaConfig load() {
        Properties properties = readPropertiesFile();
        return new AlmaConfig(
                read("ALMA_VERSION",        properties, "alma.version",        "0.0.0"),
                readEnvOnly("OPENAI_API_KEY"),
                read("ALMA_MODEL_PROBE",    properties, "alma.model.probe",    "gpt-4o-mini"),
                read("ALMA_MODEL_EXEC",     properties, "alma.model.exec",     "gpt-4o"),
                read("ALMA_LOG_LEVEL",      properties, "alma.log.level",      "INFO"),
                read("ALMA_BENCHMARK_PATH", properties, "alma.benchmark.path", "benchmarks/linguistic.json")
        );
    }

    public static void requireOpenAiKey(AlmaConfig config) {
        String key = config.openAiApiKey();
        if (key == null || key.isBlank()) {
            throw new MissingEnvException("missing OPENAI_API_KEY. Set it and retry.");
        }
    }

    private static Properties readPropertiesFile() {
        Properties properties = new Properties();
        try (InputStream stream = EnvLoader.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_PATH)) {
            if (stream != null) {
                properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // Defaults will be used.
        }
        return properties;
    }

    private static String read(String envName, Properties properties, String propertyName, String fallback) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromProperties = properties.getProperty(propertyName);
        if (fromProperties != null && !fromProperties.isBlank()) {
            return fromProperties;
        }
        return fallback;
    }

    private static String readEnvOnly(String envName) {
        String value = System.getenv(envName);
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}
