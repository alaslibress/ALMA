package alma.core;

// Checked. Thrown for any pipeline-level failure.
public class MutationException extends Exception {

    public MutationException(String message) {
        super(message);
    }

    public MutationException(String message, Throwable cause) {
        super(message, cause);
    }
}
