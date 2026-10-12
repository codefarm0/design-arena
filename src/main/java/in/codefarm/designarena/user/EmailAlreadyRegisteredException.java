package in.codefarm.designarena.user;

/** Thrown when somebody tries to register an email address that already has an account. */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account already exists for " + email);
    }
}
