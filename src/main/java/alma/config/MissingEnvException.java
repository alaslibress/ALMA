package alma.config;

// Thrown when a required environment value is missing.
public final class MissingEnvException extends RuntimeException {

    public MissingEnvException(String message) {
        super(message);
    }
}
