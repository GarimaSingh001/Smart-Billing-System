package exception;

/** Thrown when user input or data fails validation. */
public class InvalidInputException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Creates the exception with a clear message. */
    public InvalidInputException(String message) {
        super(message);
    }
}