package exception;

/** Thrown when a login or password check fails (checked, so callers must handle it). */
public class AuthenticationException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Creates the exception with a user-friendly message. */
    public AuthenticationException(String message) {
        super(message);
    }

    /** Creates the exception with a message and the underlying cause. */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
