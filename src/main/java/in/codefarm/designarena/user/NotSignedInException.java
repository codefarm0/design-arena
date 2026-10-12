package in.codefarm.designarena.user;

/**
 * Thrown when code that requires an account runs without one.
 *
 * <p>This is a programming error rather than a user error: it means a view or service
 * that needs identity was reached anonymously, which the security rules should have
 * prevented.
 */
public class NotSignedInException extends RuntimeException {

    public NotSignedInException() {
        super("This action requires a signed-in account");
    }
}
